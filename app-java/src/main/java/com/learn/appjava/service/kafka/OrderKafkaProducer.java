package com.learn.appjava.service.kafka;

import com.learn.appjava.event.OrderCreatedEvent;
import com.learn.appjava.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaProducer {

    private static final String TOPIC = "order-created";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Gửi event order-created vào Kafka
     * Key = orderId → đảm bảo các event của cùng 1 order vào cùng 1 partition
     * → đảm bảo thứ tự xử lý: created → processing → completed
     *
     * Dùng Kafka thay vì RabbitMQ vì:
     * - Cần audit log: biết order đã qua những bước nào
     * - Cần replay: nếu InventoryService down, recover xong đọc lại từ offset
     * - Nhiều service cần đọc cùng 1 event (inventory, analytics, reporting)
     */
    public void sendOrderCreated(Order order, String userEmail) {
        String key = String.valueOf(order.getId());
        OrderCreatedEvent event = new OrderCreatedEvent(
                key,
                userEmail,
                order.getProduct(),
                order.getQuantity()
        );
        kafkaTemplate.send(TOPIC, key, event);
        log.info(">>> [Kafka] Sent order-created event: {}", event);
    }
}