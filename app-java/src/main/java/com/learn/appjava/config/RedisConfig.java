package com.learn.appjava.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Cấu hình RedisTemplate để tương tác với Redis.
 *
 * Mặc định Spring serialize data dạng byte array (JdkSerializationRedisSerializer)
 * → khó đọc khi debug, không tương thích nếu dùng nhiều ngôn ngữ khác nhau.
 * Config này override serializer để dễ đọc và linh hoạt hơn.
 */
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Key serialize thành plain String: "user:1", "product:99"
        // Dễ đọc, dễ filter khi debug trên Redis Insight / redis-cli
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value serialize thành JSON thay vì byte array
        // Cần custom ObjectMapper để thêm type info vào JSON
        ObjectMapper objectMapper = new ObjectMapper();

        // activateDefaultTyping → thêm class name vào JSON khi serialize
        // VD: ["com.learn.appjava.model.User", {"id":1,"name":"John"}]
        // Cần thiết để deserialize đúng kiểu Object khi đọc ra từ Redis
        // NON_FINAL: áp dụng cho tất cả class không phải final (String, Integer thì không cần)
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        return template;
    }

    /**
     * CacheManager — Spring Cache Abstraction dùng bean này để biết
     * sẽ cache vào đâu (Redis, Caffeine, EhCache,...)
     * Nếu không config thì @Cacheable sẽ không biết dùng Redis.
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))  // TTL mặc định
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer())
                );

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }
}