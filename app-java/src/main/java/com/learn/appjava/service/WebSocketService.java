package com.learn.appjava.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyOrderStatus(Long userId, String orderId, String status, String message) {
        String destination = "/topic/orders/" + userId;
        OrderStatusMessage payload = new OrderStatusMessage(orderId, status, message);
        messagingTemplate.convertAndSend(destination, payload);
        log.info(">>> [WebSocket] Pushed to {}: {}", destination, payload);
    }

    public record OrderStatusMessage(String orderId, String status, String message) {}
}