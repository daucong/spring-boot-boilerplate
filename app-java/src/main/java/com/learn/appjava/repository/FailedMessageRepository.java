package com.learn.appjava.repository;

import com.learn.appjava.model.FailedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FailedMessageRepository extends JpaRepository<FailedMessage, Long> {

    // Lấy tất cả message chưa xử lý để team review
    List<FailedMessage> findByStatus(FailedMessage.FailedMessageStatus status);
}