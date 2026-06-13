package com.learn.appjava.event;

public record OrderCreatedEvent(
        String orderId,
        String userEmail,
        String productName,
        int quantity
) {}