package com.learn.appjava.controller;

import com.learn.appjava.model.FailedMessage;
import com.learn.appjava.repository.FailedMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/failed-messages")
@RequiredArgsConstructor
public class FailedMessageController {

    private final FailedMessageRepository failedMessageRepository;

    // Xem tất cả message đang PENDING — team dùng để review
    @GetMapping
    public ResponseEntity<List<FailedMessage>> getPending() {
        return ResponseEntity.ok(
                failedMessageRepository.findByStatus(FailedMessage.FailedMessageStatus.PENDING)
        );
    }

    // Team fix lỗi xong → mark RESOLVED
    @PatchMapping("/{id}/resolve")
    public ResponseEntity<FailedMessage> resolve(@PathVariable Long id) {
        FailedMessage msg = failedMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found: " + id));
        msg.setStatus(FailedMessage.FailedMessageStatus.RESOLVED);
        return ResponseEntity.ok(failedMessageRepository.save(msg));
    }
}