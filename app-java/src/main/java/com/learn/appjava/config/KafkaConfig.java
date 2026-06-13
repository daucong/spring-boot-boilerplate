package com.learn.appjava.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    // Tạo topic "user-events" với 3 partition, 1 replica
    // 3 partition → tối đa 3 consumer xử lý song song
    // 1 replica → local thôi, production thường 3
    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name("user-events")
                .partitions(3)
                .replicas(1)
                .build();
    }
}