package com.learn.appjava.service.rabbitmq;

import com.learn.appjava.config.RabbitMQConfig;
import com.learn.appjava.model.Order;
import com.learn.appjava.repository.OrderRepository;
import com.learn.appjava.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventRabbitConsumer {

    private final OrderRepository orderRepository;
    private final WebSocketService webSocketService;

    /**
     * Direct Exchange — 1 worker xử lý thanh toán
     * Nếu có nhiều instance app → RabbitMQ tự round-robin
     * Mỗi order chỉ được 1 worker xử lý
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "order.payment.queue", durable = "true"),
            exchange = @Exchange(name = RabbitMQConfig.ORDER_DIRECT_EXCHANGE, type = ExchangeTypes.DIRECT),
            key = RabbitMQConfig.ORDER_PAYMENT_KEY
    ))
    public void handlePayment(@Payload Order order) {
        log.info(">>> [Direct/Payment] Processing payment for order: {}", order);
        orderRepository.findById(order.getId()).ifPresent(o -> {
            o.setStatus(Order.OrderStatus.COMPLETED);
            orderRepository.save(o);
            log.info(">>> [Direct/Payment] Order {} status updated to COMPLETED", o.getId());
            
            webSocketService.notifyOrderStatus(
                    o.getUserId(),
                    o.getId().toString(),
                    "COMPLETED",
                    "Đơn hàng đã hoàn thành ✅"
            );
        });
    }

    /**
     * Fanout Exchange — Email service nhận
     * Độc lập với Notification service
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "order.email.queue", durable = "true"),
            exchange = @Exchange(name = RabbitMQConfig.ORDER_FANOUT_EXCHANGE, type = ExchangeTypes.FANOUT)
    ))
    public void handleEmail(@Payload Order order) {
        log.info(">>> [Fanout/Email] Sending email for order: {}", order);
        // Thực tế: gọi email service gửi mail xác nhận
    }

    /**
     * Fanout Exchange — Notification service nhận
     * Độc lập với Email service
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "order.notification.queue", durable = "true"),
            exchange = @Exchange(name = RabbitMQConfig.ORDER_FANOUT_EXCHANGE, type = ExchangeTypes.FANOUT)
    ))
    public void handleNotification(@Payload Order order) {
        log.info(">>> [Fanout/Notification] Pushing notification for order: {}", order);
        // Thực tế: gọi push notification service
    }

    /**
     * Topic Exchange — Order team nhận tất cả log "order.#"
     * Match: order.created.production, order.created.staging, order.failed.production
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "order.log.queue", durable = "true"),
            exchange = @Exchange(name = RabbitMQConfig.ORDER_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = "order.#"
    ))
    public void handleOrderLog(@Payload Order order) {
        log.info(">>> [Topic/OrderLog] Order team log: {}", order);
    }

    /**
     * Topic Exchange — DevOps chỉ nhận log production "*.*.production"
     * Match: order.created.production, order.failed.production
     * Không match: order.created.staging
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "order.devops.log.queue", durable = "true"),
            exchange = @Exchange(name = RabbitMQConfig.ORDER_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = "*.*.production"
    ))
    public void handleDevopsLog(@Payload Order order) {
        log.info(">>> [Topic/DevopsLog] DevOps production log: {}", order);
    }
}