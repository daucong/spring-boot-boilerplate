package com.learn.appjava.service;

import com.learn.appjava.model.Order;
import com.learn.appjava.model.User;
import com.learn.appjava.repository.OrderRepository;
import com.learn.appjava.repository.UserRepository;
import com.learn.appjava.service.kafka.OrderKafkaProducer;
import com.learn.appjava.service.rabbitmq.OrderEventRabbitProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderKafkaProducer orderKafkaProducer;
    private final OrderEventRabbitProducer orderRabbitProducer;
    private final UserRepository userRepository;

    public Order create(Order order) {
        // 1. Lưu DB với status PENDING
        order.setStatus(Order.OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        log.info(">>> Order created: {}", saved);

        // 2. Kafka → gửi event "order-created" để InventoryService trừ kho
        // Dùng Kafka vì cần audit log, cần replay lại nếu có lỗi
        String userEmail = userRepository.findById(saved.getUserId())
                .map(User::getEmail)
                .orElse(null);
        orderKafkaProducer.sendOrderCreated(saved, userEmail);

        // 3. RabbitMQ Direct → 1 worker xử lý thanh toán
        // Dùng RabbitMQ vì chỉ cần 1 worker xử lý, không cần lưu lại message
        orderRabbitProducer.sendDirect(saved);

        // 4. RabbitMQ Fanout → broadcast email + notification
        // Dùng RabbitMQ Fanout vì nhiều service cần nhận cùng lúc
        orderRabbitProducer.sendFanout(saved);

        // 5. RabbitMQ Topic → log theo pattern
        orderRabbitProducer.sendTopic(saved, "order.created.production");

        return saved;
    }

    public List<Order> getByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }
}