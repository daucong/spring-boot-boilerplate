package com.learn.appjava.service.kafka;

import com.learn.appjava.event.OrderCreatedEvent;
import com.learn.appjava.model.Order;
import com.learn.appjava.model.Product;
import com.learn.appjava.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaConsumer {

    private final ProductRepository productRepository;

    /**
     * Nhận event order-created từ Kafka
     * Trừ kho trong DB — đây là bước sync DB sau khi Redis đã trừ
     * Dùng @Transactional để đảm bảo nếu lỗi thì rollback
     */
    @Transactional
    @KafkaListener(topics = "order-created", groupId = "inventory-group")
    public void handleOrderCreated(@Payload OrderCreatedEvent event) {
        log.info(">>> [Kafka/Inventory] Received event: {}", event);

        productRepository.findAll().stream()
                .filter(p -> p.getName().equals(event.productName()))
                .findFirst()
                .ifPresent(product -> {
                    if (product.getStock() > 0) {
                        product.setStock(product.getStock() - 1);
                        productRepository.save(product);
                        log.info(">>> [Kafka/Inventory] Decremented DB stock: product={} remaining={}",
                                product.getName(), product.getStock());
                    }
                });
    }
}