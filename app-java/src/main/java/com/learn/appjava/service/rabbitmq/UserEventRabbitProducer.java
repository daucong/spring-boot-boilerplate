package com.learn.appjava.service.rabbitmq;

import com.learn.appjava.config.RabbitMQConfig;
import com.learn.appjava.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventRabbitProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Direct Exchange — gửi đúng vào queue binding với key "user.created"
     * Chỉ 1 queue nhận message này
     */
    public void sendDirect(User user) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.USER_DIRECT_EXCHANGE,
                RabbitMQConfig.USER_CREATED_KEY,
                user
        );
        log.info(">>> [Direct] Sent user event: {}", user);
    }

    /**
     * Fanout Exchange — broadcast tất cả queue binding
     * Tất cả queue đều nhận, không cần routing key
     */
    public void sendFanout(User user) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.USER_FANOUT_EXCHANGE,
                "", // fanout không cần routing key
                user
        );
        log.info(">>> [Fanout] Sent user event: {}", user);
    }

    /**
     * Topic Exchange — gửi với key "user.created"
     * Queue binding với pattern "user.#" sẽ nhận
     */
    public void sendTopic(User user) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.USER_TOPIC_EXCHANGE,
                RabbitMQConfig.USER_CREATED_KEY,
                user
        );
        log.info(">>> [Topic] Sent user event: {}", user);
    }
}
