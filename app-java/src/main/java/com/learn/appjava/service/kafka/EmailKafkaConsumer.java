package com.learn.appjava.service.kafka;

import com.learn.appjava.event.OrderCreatedEvent;
import com.learn.appjava.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailKafkaConsumer {

    private final EmailService emailService;

    @KafkaListener(topics = "order-created", groupId = "email-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("EmailKafkaConsumer received: orderId={}", event.orderId());
        emailService.sendOrderConfirmation(
                event.userEmail(),
                event.orderId(),
                event.productName(),
                event.quantity()
        );
    }
}