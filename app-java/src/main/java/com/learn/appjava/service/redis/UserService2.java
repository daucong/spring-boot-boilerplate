package com.learn.appjava.service.redis;

import com.learn.appjava.model.User;
import com.learn.appjava.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService2 {

    private final UserRepository userRepository;

    /**
     * @Cacheable — Kiểm tra cache trước, nếu HIT thì return luôn, không chạy method body.
     * Nếu MISS thì chạy method, lưu kết quả vào cache rồi return.
     * value = "users" → tên cache (key sẽ là "users::1", "users::2",...)
     * key = "#id"     → Spring Expression Language, lấy param id làm cache key
     */
    @Cacheable(value = "users", key = "#id")
    public User getById(Long id) {
        System.out.println(">>> MISS cache, query DB: " + id);
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    /**
     * @CachePut — Luôn chạy method body (update DB), sau đó update cache luôn.
     * Khác @Cacheable ở chỗ: không skip method dù cache đang có data.
     * Dùng cho UPDATE — vừa save DB vừa đồng bộ cache.
     */
    @CachePut(value = "users", key = "#id")
    public User update(Long id, User request) {
        System.out.println(">>> UPDATE DB + UPDATE cache: " + id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        return userRepository.save(user);
    }

    /**
     * @CacheEvict — Xóa cache sau khi method chạy xong.
     * Dùng cho DELETE — xóa DB xong xóa cache luôn.
     */
    @CacheEvict(value = "users", key = "#id")
    public void delete(Long id) {
        System.out.println(">>> DELETE DB + EVICT cache: " + id);
        userRepository.deleteById(id);
    }

    public User create(User user) {
        return userRepository.save(user);
    }
}