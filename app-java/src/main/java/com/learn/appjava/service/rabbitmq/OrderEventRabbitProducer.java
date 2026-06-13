package com.learn.appjava.service.rabbitmq;

import com.learn.appjava.config.RabbitMQConfig;
import com.learn.appjava.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventRabbitProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Direct Exchange → order.payment.queue
     * Chỉ 1 worker xử lý thanh toán cho mỗi order
     * Dùng Direct vì:
     * - Task cụ thể: xử lý thanh toán
     * - Chỉ 1 worker được xử lý, không duplicate
     * - Nếu có 3 worker, RabbitMQ tự round-robin chia đều
     */
    public void sendDirect(Order order) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_DIRECT_EXCHANGE,
                RabbitMQConfig.ORDER_PAYMENT_KEY,
                order
        );
        log.info(">>> [RabbitMQ Direct] Sent order payment: {}", order);
    }

    /**
     * Fanout Exchange → email.queue + notification.queue
     * Cả 2 service nhận cùng lúc, độc lập nhau
     * Dùng Fanout vì:
     * - Nhiều service cần nhận cùng 1 event
     * - Email down không ảnh hưởng Notification
     * - Thêm service mới chỉ cần bind queue, không sửa producer
     */
    public void sendFanout(Order order) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_FANOUT_EXCHANGE,
                "",
                order
        );
        log.info(">>> [RabbitMQ Fanout] Sent order broadcast: {}", order);
    }

    /**
     * Topic Exchange → log theo pattern "order.created.production"
     * Queue binding "order.#" sẽ nhận tất cả log của order service
     * Queue binding "*.*.production" chỉ nhận log production
     * Dùng Topic vì:
     * - Routing linh hoạt theo nhiều tiêu chí
     * - DevOps chỉ muốn nhận log production
     * - Order team muốn nhận tất cả log của order
     */
    public void sendTopic(Order order, String routingKey) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_TOPIC_EXCHANGE,
                routingKey,
                order
        );
        log.info(">>> [RabbitMQ Topic] Sent order log key={}: {}", routingKey, order);
    }
}