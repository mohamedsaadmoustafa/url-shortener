package com.example.shortener.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Publishes URL click events to Kafka.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Send a click event for a short URL key.
     */
    public void publishClick(String key, String ip, String ua, String referer) {
        try {
            Map<String, Object> payload = Map.of(
                    "key", key,
                    "ip", ip == null ? "" : ip,
                    "ua", ua == null ? "" : ua,
                    "referer", referer == null ? "" : referer,
                    "ts", Instant.now().toString()
            );

            String json = mapper.writeValueAsString(payload);
            kafka.send("clicks", key, json);

            log.info("Published click for key '{}'", key);
        } catch (Exception e) {
            log.error("Failed to publish click for '{}'", key, e);
        }
    }
}
