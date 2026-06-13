package com.learn.appjava.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "failed_messages")
public class FailedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Topic DLT nhận được
    private String topic;

    // Nội dung message gốc lưu dạng JSON string
    @Column(columnDefinition = "TEXT")
    private String payload;

    // Lý do fail
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    // Trạng thái: PENDING → team xử lý → RESOLVED
    @Enumerated(EnumType.STRING)
    private FailedMessageStatus status;

    private LocalDateTime createdAt;

    public enum FailedMessageStatus {
        PENDING, RESOLVED
    }
}