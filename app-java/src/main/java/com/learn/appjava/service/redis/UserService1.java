package com.learn.appjava.service.redis;

import com.learn.appjava.service.kafka.UserEventProducer;
import com.learn.appjava.model.User;
import com.learn.appjava.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class UserService1 {

    private static final String CACHE_KEY = "user:";
    private static final String LOCK_KEY = "lock:user:";

    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserEventProducer userEventProducer;

    private static long randomTtl() {
        return 300 + (long)(Math.random() * 120);
    }

    public User getById(Long id) {
        String cacheKey = CACHE_KEY + id;
        String lockKey = LOCK_KEY + id;

        // 1. Check cache
        User cached = (User) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            System.out.println(">>> HIT cache: " + cacheKey);
            return cached;
        }

        // 2. MISS → thử acquire lock
        // setIfAbsent = SET NX EX → chỉ set nếu key chưa tồn tại
        // Đảm bảo chỉ 1 request "thắng" được lock
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(10));

        if (Boolean.TRUE.equals(locked)) {
            try {
                // 3. Thắng lock → query DB → set cache
                System.out.println(">>> Acquired lock, query DB: " + id);
                // Giả lập query DB chậm 2 giây
                Thread.sleep(2000);

                User user = userRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("User not found: " + id));
                redisTemplate.opsForValue().set(cacheKey, user, Duration.ofSeconds(randomTtl()));
                return user;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                // 4. Luôn release lock trong finally — dù có exception cũng phải release
                redisTemplate.delete(lockKey);
                System.out.println(">>> Released lock: " + id);
            }
        } else {
            // 5. Thua lock → chờ 100ms rồi retry
            // Lần retry này cache đã có data rồi → sẽ HIT
            System.out.println(">>> Waiting for lock, retry: " + id);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return getById(id);
        }
    }

    public User create(User user) {
        User saved = userRepository.save(user);

        // Lưu cache ngay sau khi tạo với random TTL
        String key = CACHE_KEY + saved.getId();
        redisTemplate.opsForValue().set(key, saved, Duration.ofSeconds(randomTtl()));
        System.out.println(">>> SET cache: " + key + " TTL: " + randomTtl() + "s");

        // Gửi event Kafka
        userEventProducer.sendUserCreated(saved);

        return saved;
    }

    public User update(Long id, User request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        userRepository.save(user);

        // Xóa cache → lần GET tiếp theo sẽ MISS và lấy data mới từ DB
        String key = CACHE_KEY + id;
        redisTemplate.delete(key);
        System.out.println(">>> DELETE cache: " + key);

        return user;
    }

    public void delete(Long id) {
        userRepository.deleteById(id);

        String key = CACHE_KEY + id;
        redisTemplate.delete(key);
        System.out.println(">>> DELETE cache: " + key);
    }
}