package com.example.shortener.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClickBatchConsumer {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String HASH_KEY = "clicks";

    @KafkaListener(topics = "clicks", groupId = "clicks-group")
    public void consume(String message) {
        try {
            Map<String, Object> data = mapper.readValue(message, Map.class);
            String shortKey = (String) data.get("key");

            if (shortKey == null || shortKey.isBlank()) return;

            redisTemplate.opsForHash().increment(HASH_KEY, shortKey, 1L);
            redisTemplate.expire(HASH_KEY, 2, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("Failed to process click payload -> {}", message, e);
        }
    }
}
