package com.example.shortener.filter;

import com.example.shortener.entity.AbuseEvent;
import com.example.shortener.entity.AbuseEventType;
import com.example.shortener.properties.RateLimitProperties;
import com.example.shortener.service.AbuseEventService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

/**
 * RateLimit + Abuse Detection filter.
 * Detects excessive POST and GET requests.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter implements Filter {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RateLimitProperties rateLimitProperties;
    private final AbuseEventService abuseService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if (!isRateLimitedEndpoint(req)) {
            chain.doFilter(request, response);
            return;
        }

        String ip = req.getRemoteAddr();
        AbuseEventType eventType = determineEventType(req);
        int maxTokens = determineMaxTokens(eventType);
        String shortKey = extractShortKey(req);

        if (handleRequest(ip, eventType, maxTokens, shortKey)) {
            chain.doFilter(request, response);
        } else {
            res.setStatus(429); // 429 = Too Many Requests
            res.getWriter().write("Rate limit exceeded. Your activity has been flagged.");
        }
    }

    private boolean isRateLimitedEndpoint(HttpServletRequest req) {
        return ("POST".equalsIgnoreCase(req.getMethod()) && req.getRequestURI().equalsIgnoreCase("/api/shorten")) ||
                ("GET".equalsIgnoreCase(req.getMethod()) && req.getRequestURI().startsWith("/r/"));
    }

    private AbuseEventType determineEventType(HttpServletRequest req) {
        return "POST".equalsIgnoreCase(req.getMethod()) ? AbuseEventType.EXCESSIVE_POSTS : AbuseEventType.EXCESSIVE_GETS;
    }

    private int determineMaxTokens(AbuseEventType eventType) {
        return eventType == AbuseEventType.EXCESSIVE_POSTS ?
                rateLimitProperties.getPostMaxTokens() :
                rateLimitProperties.getGetMaxTokens();
    }

    private String extractShortKey(HttpServletRequest req) {
        if ("GET".equalsIgnoreCase(req.getMethod()) && req.getRequestURI().startsWith("/r/")) {
            return req.getRequestURI().substring("/r/".length());
        }
        return null; // For POST, no specific shortKey
    }

    /**
     * Handles token consumption and abuse detection.
     *
     * @return true if request allowed, false if abuse detected
     */
    private boolean handleRequest(String ip, AbuseEventType eventType, int maxTokens, String shortKey) {
        String key = "ratelimit:" + eventType.name().toLowerCase() + ":" + ip;

        Object obj = redisTemplate.opsForValue().get(key);
        Long tokensLeft = obj != null ? Long.parseLong(obj.toString()) : null;

        if (tokensLeft == null) {
            tokensLeft = (long) maxTokens;
            redisTemplate.opsForValue().set(key, tokensLeft, Duration.ofSeconds(rateLimitProperties.getRefillIntervalSeconds()));
        }

        tokensLeft--;
        redisTemplate.opsForValue().set(key, tokensLeft, Duration.ofSeconds(rateLimitProperties.getRefillIntervalSeconds()));

        if (tokensLeft < 0) {
            recordAbuseEvent(ip, eventType, shortKey);
            return false;
        }

        return true;
    }

    private void refillBucket(String key, int maxTokens) {
        redisTemplate.opsForValue().set(
                key,
                maxTokens,
                Duration.ofSeconds(rateLimitProperties.getRefillIntervalSeconds())
        );
        log.debug("Refilled bucket '{}' with {} tokens", key, maxTokens);
    }

    private Long consumeToken(String key) {
        Long tokensLeft = redisTemplate.opsForValue().decrement(key);
        log.debug("Consumed token for '{}', tokens left: {}", key, tokensLeft);
        return tokensLeft;
    }

    private void recordAbuseEvent(String ip, AbuseEventType eventType, String shortKey) {
        AbuseEvent event = AbuseEvent.builder()
                .shortKey(shortKey)
                .eventType(eventType)
                .ipAddress(ip)
                .userAgent("") // Could extract from request if needed
                .referer("Exceeded max tokens in RateLimitFilter")
                .createdAt(java.time.Instant.now())
                .build();
        abuseService.recordEvent(event);
        log.warn("Abuse event recorded: {} for IP {}", eventType, ip);
    }
}
