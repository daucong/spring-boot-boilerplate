package com.learn.appjava.service.kafka;

import com.learn.appjava.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventProducer {

    private static final String TOPIC = "user-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // key = user ID → đảm bảo các event của cùng 1 user vào cùng 1 partition
    // → đảm bảo thứ tự xử lý event của 1 user
    public void sendUserCreated(User user) {
        String key = String.valueOf(user.getId());
        kafkaTemplate.send(TOPIC, key, user);
        log.info(">>> Sent user-created event: {}", user);
    }
}