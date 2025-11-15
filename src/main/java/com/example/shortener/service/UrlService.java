package com.example.shortener.service;

import com.example.shortener.properties.AppProperties;
import com.example.shortener.entity.Url;
import com.example.shortener.kafka.EventPublisher;
import com.example.shortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Central service for URL management.
 *
 * This service coordinates:
 * - URL creation.
 * - URL resolution.
 * - Click event publishing.
 * - Metadata caching for partitioning.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UrlService {

    private final UrlCreationService creationService;
    private final UrlResolutionService resolutionService;
    private final EventPublisher eventPublisher;
    private final UrlRepository urlRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AppProperties appProperties;

    private static final String URL_CACHE_PREFIX = "url:meta:";

    /**
     * Retrieves a URL entity by its short key.
     * First checks Redis for cached creation time to narrow down
     * the partition search, then queries the database.
     *
     * @param shortKey the shortened key
     * @return Optional containing the found URL, if any
     */
    public Optional<Url> getByShortKey(String shortKey) {
        String cacheKey = URL_CACHE_PREFIX + shortKey;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                Instant createdAt = Instant.parse(cached.toString());
                return urlRepository.findActiveByShortKeyInRange(
                        shortKey,
                        createdAt.minusSeconds(86400),
                        createdAt.plusSeconds(366L * 86400)
                );
            }

            Optional<Url> result = urlRepository.findByShortKeyAndDeletedAtIsNull(shortKey);
            result.ifPresent(url -> {
                redisTemplate.opsForValue().set(
                        cacheKey,
                        url.getCreatedAt().toString(),
                        appProperties.getCacheTtlSeconds(),
                        TimeUnit.SECONDS
                );
            });
            return result;
        } catch (Exception e) {
            log.error("Error fetching key {}: {}", shortKey, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Creates a short URL and records blacklist or abuse events.
     *
     * @param originalUrl original URL
     * @param customAlias optional custom alias
     * @param expiresAt   optional expiration timestamp
     * @param ipAddress   client IP for abuse logging
     * @param userAgent   client User-Agent for abuse logging
     * @return the saved {@link Url} entity
     */
    @Transactional
    public Url createShortUrl(String originalUrl, String customAlias, Instant expiresAt,
                              String ipAddress, String userAgent) {
        return creationService.createShortUrl(originalUrl, customAlias, expiresAt, ipAddress, userAgent);
    }

    /**
     * Resolves a short key to its corresponding long URL.
     *
     * @param shortKey the short key
     * @return Optional containing the resolved {@link Url}, if found
     */
    public Optional<Url> resolve(String shortKey) {
        return resolutionService.resolve(shortKey);
    }

    /**
     * Publishes a click-tracking event to Kafka.
     *
     * @param shortKey the clicked short key
     * @param ip       client IP
     * @param ua       user agent
     * @param referer  referring page
     */
    public void publishClickEvent(String shortKey, String ip, String ua, String referer) {
        try {
            eventPublisher.publishClick(shortKey, ip, ua, referer);
        } catch (Exception e) {
            log.warn("Failed to publish click event for '{}': {}", shortKey, e.getMessage());
        }
    }
}
