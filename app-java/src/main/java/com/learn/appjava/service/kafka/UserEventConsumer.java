package com.learn.appjava.service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.appjava.model.FailedMessage;
import com.learn.appjava.model.User;
import com.learn.appjava.repository.FailedMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final FailedMessageRepository failedMessageRepository;
    private final ObjectMapper objectMapper;

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(topics = "user-events", groupId = "app-java-group")
    public void handleUserEvent(
            @Payload User user,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info(">>> Received user event | partition: {} | offset: {} | user: {}",
                partition, offset, user);
    }

    /**
     * Khi message vào DLT:
     * 1. Log lỗi
     * 2. Lưu vào DB bảng failed_messages với status PENDING
     * 3. Team review → fix lỗi → update status RESOLVED + reprocess
     */
    @DltHandler
    public void handleDlt(
            @Payload User user,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage
    ) {
        log.error(">>> DLT received | topic: {} | error: {} | user: {}", topic, errorMessage, user);

        try {
            String payload = objectMapper.writeValueAsString(user);

            FailedMessage failedMessage = FailedMessage.builder()
                    .topic(topic)
                    .payload(payload)
                    .errorMessage(errorMessage)
                    .status(FailedMessage.FailedMessageStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            failedMessageRepository.save(failedMessage);
            log.info(">>> Saved to failed_messages table: {}", failedMessage);

        } catch (Exception e) {
            log.error(">>> Failed to save failed_message: {}", e.getMessage());
        }
    }
}