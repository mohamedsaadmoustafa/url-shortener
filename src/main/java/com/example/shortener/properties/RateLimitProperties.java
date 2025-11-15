package com.example.shortener.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for rate limiting.
 * Values can be set in application.yml or application.properties.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ratelimit")
public class RateLimitProperties {

    /**
     * Maximum allowed POST tokens per IP.
     */
    private int postMaxTokens = 20;

    /**
     * Maximum allowed GET tokens per IP.
     */
    private int getMaxTokens = 20;

    /**
     * Token refill interval in seconds.
     */
    private int refillIntervalSeconds = 60;
}