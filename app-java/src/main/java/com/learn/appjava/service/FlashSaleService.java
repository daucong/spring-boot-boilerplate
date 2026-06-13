package com.learn.appjava.service;

import com.learn.appjava.model.Order;
import com.learn.appjava.model.Product;
import com.learn.appjava.model.User;
import com.learn.appjava.repository.OrderRepository;
import com.learn.appjava.repository.ProductRepository;
import com.learn.appjava.repository.UserRepository;
import com.learn.appjava.service.kafka.OrderKafkaProducer;
import com.learn.appjava.service.rabbitmq.OrderEventRabbitProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleService {

    private static final String STOCK_KEY = "flash:stock:";
    private static final String LOCK_KEY = "flash:lock:";

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderKafkaProducer orderKafkaProducer;
    private final OrderEventRabbitProducer orderRabbitProducer;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WebSocketService webSocketService;

    /**
     * Khởi tạo tồn kho trong Redis trước khi flash sale bắt đầu
     * Gọi 1 lần duy nhất trước khi mở sale
     * Redis sẽ là nơi check tồn kho nhanh thay vì query DB
     */
    public void initStock(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        String key = STOCK_KEY + productId;
        redisTemplate.opsForValue().set(key, product.getStock(), Duration.ofHours(24));
        log.info(">>> Init stock in Redis: product={} stock={}", productId, product.getStock());
    }

    /**
     * Xử lý đặt hàng flash sale
     * Flow:
     * 1. Distributed Lock — tránh race condition
     * 2. Check tồn kho Redis — nhanh, không query DB
     * 3. Trừ kho Redis
     * 4. Lưu order DB status PENDING
     * 5. Kafka — audit log tồn kho
     * 6. RabbitMQ Direct — payment worker xử lý
     * 7. RabbitMQ Fanout — email + notification
     */
    public Order placeOrder(Long productId, Long userId) {
        String stockKey = STOCK_KEY + productId;
        String lockKey = LOCK_KEY + productId;

        // 1. Acquire Distributed Lock
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(10));

        if (!Boolean.TRUE.equals(locked)) {
            throw new RuntimeException("Server busy, please try again");
        }

        try {
            // 2. Check tồn kho Redis
            Integer stock = (Integer) redisTemplate.opsForValue().get(stockKey);
            log.info(">>> Check stock: product={} stock={}", productId, stock);

            if (stock == null || stock <= 0) {
                throw new RuntimeException("Out of stock");
            }

            // 3. Trừ kho Redis
            redisTemplate.opsForValue().decrement(stockKey);
            log.info(">>> Decremented stock in Redis: product={} remaining={}", productId, stock - 1);

            // 4. Lưu order DB
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            Order order = Order.builder()
                    .userId(userId)
                    .product(product.getName())
                    .quantity(1)
                    .price(product.getPrice())
                    .status(Order.OrderStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            Order saved = orderRepository.save(order);
            log.info(">>> Order saved: {}", saved);

            // Notify user: order đang PENDING
            webSocketService.notifyOrderStatus(
                    userId,
                    saved.getId().toString(),
                    "PENDING",
                    "Đơn hàng đang xử lý..."
            );

            // 5. Kafka → InventoryService trừ kho DB (async, audit log)
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            orderKafkaProducer.sendOrderCreated(saved, user.getEmail());

            // 6. RabbitMQ Direct → PaymentService (1 worker xử lý)
            orderRabbitProducer.sendDirect(saved);

            // 7. RabbitMQ Fanout → Email + Notification
            orderRabbitProducer.sendFanout(saved);

            return saved;

        } catch (RuntimeException e) {
            // Nếu lỗi sau khi đã trừ kho Redis → hoàn kho lại
            if (!(e.getMessage().equals("Out of stock"))) {
                redisTemplate.opsForValue().increment(stockKey);
                log.warn(">>> Rolled back stock in Redis: product={}", productId);
            }
            throw e;
        } finally {
            // Luôn release lock
            redisTemplate.delete(lockKey);
            log.info(">>> Released lock: product={}", productId);
        }
    }
}