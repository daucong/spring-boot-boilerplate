package com.learn.appjava.service.rabbitmq;

import com.learn.appjava.config.RabbitMQConfig;
import com.learn.appjava.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserEventRabbitConsumer {

    /**
     * Lắng nghe user.queue — nhận message từ:
     * - Direct exchange với key "user.created"
     * - Fanout exchange (broadcast)
     * - Topic exchange với key match "user.#"
     */
    @RabbitListener(queues = RabbitMQConfig.USER_QUEUE)
    public void handleUserQueue(@Payload User user) {
        log.info(">>> [user.queue] Received: {}", user);
    }

    /**
     * Lắng nghe user.notification.queue — chỉ nhận từ Fanout exchange
     * Simulate notification service nhận event
     */
    @RabbitListener(queues = RabbitMQConfig.USER_NOTIFICATION_QUEUE)
    public void handleNotificationQueue(@Payload User user) {
        log.info(">>> [user.notification.queue] Received: {}", user);
    }
}
