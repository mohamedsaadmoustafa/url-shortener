package com.example.shortener.service;

import com.example.shortener.properties.AppProperties;
import com.example.shortener.entity.Url;
import com.example.shortener.kafka.EventPublisher;
import com.example.shortener.repository.UrlRepository;
import com.example.shortener.util.KeyGenerator;
import com.example.shortener.util.UrlValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Central service for URL management.
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

    private final KeyGenerator keyGenerator;
    private final UrlValidator urlValidator;
    private final EventPublisher eventPublisher;
    private final UrlRepository urlRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AppProperties appProperties;

    private static final String URL_CACHE_PREFIX = "url:";
    private static final int MAX_GENERATION_ATTEMPTS = 5;

    /**
     * Retrieves a URL entity by its short key.
     * Uses caching for better performance.
     *
     * @param shortKey the shortened key
     * @return Optional containing the found URL, if any
     */
    public Optional<Url> getByShortKey(String shortKey) {
        String cacheKey = URL_CACHE_PREFIX + shortKey;

        try {
            // Try cache first
            Optional<Url> cachedUrl = getFromCache(cacheKey);
            if (cachedUrl.isPresent()) {
                return cachedUrl;
            }

            // Cache miss - query database
            Optional<Url> dbUrl = urlRepository.findByShortKeyAndDeletedAtIsNull(shortKey);
            dbUrl.ifPresent(url -> cacheUrl(cacheKey, url));
            return dbUrl;

        } catch (Exception e) {
            log.error("Error fetching key '{}': {}", shortKey, e.getMessage());
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

        // Validate URL with abuse detection
        urlValidator.validate(originalUrl, ipAddress, userAgent);

        if (customAlias != null && !customAlias.isBlank()) {
            return createCustomAlias(originalUrl, customAlias, expiresAt);
        }
        return createGeneratedAlias(originalUrl, expiresAt);
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

    /**
     * Resolves the given short key to its corresponding URL entity.
     * Includes active status validation and caching.
     *
     * @param shortKey the short key to resolve
     * @return an Optional containing the resolved {@link Url}, or empty if not found or inactive
     */
    public Optional<Url> resolve(String shortKey) {
        String cacheKey = URL_CACHE_PREFIX + shortKey;

        // Try cache first
        Optional<Url> cachedUrl = getFromCache(cacheKey);
        if (cachedUrl.isPresent()) {
            Url url = cachedUrl.get();
            if (urlValidator.isActive(url)) {
                log.debug("✅ Cache hit for active URL '{}'", shortKey);
                return cachedUrl;
            } else {
                log.debug("❌ Cached URL '{}' is inactive", shortKey);
                return Optional.empty();
            }
        }

        // Cache miss - query database
        log.debug("Querying DB for key '{}'", shortKey);
        Optional<Url> dbUrl = urlRepository.findByShortKeyAndDeletedAtIsNull(shortKey)
                .filter(urlValidator::isActive);

        if (dbUrl.isPresent()) {
            Url url = dbUrl.get();
            cacheUrl(cacheKey, url);
            log.debug("✅ Found and cached active URL '{}'", shortKey);
        } else {
            log.debug("❌ URL not found or inactive: '{}'", shortKey);
        }

        return dbUrl;
    }

    private Url createCustomAlias(String originalUrl, String alias, Instant expiresAt) {
        if (urlRepository.existsByShortKey(alias)) {
            log.warn("Custom alias '{}' already exists", alias);
            throw new IllegalArgumentException("Alias already in use");
        }

        Url url = buildUrl(originalUrl, alias, true, expiresAt);
        Url saved = saveAndCache(url);
        log.info("✅ Saved custom alias '{}'", saved.getShortKey());
        return saved;
    }

    private Url createGeneratedAlias(String originalUrl, Instant expiresAt) {
        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            String key = keyGenerator.generate();

            if (urlRepository.existsByShortKey(key)) {
                log.debug("Attempt {}/{}: Key '{}' already exists, retrying...",
                        attempt, MAX_GENERATION_ATTEMPTS, key);
                continue;
            }

            Url url = buildUrl(originalUrl, key, false, expiresAt);
            Url saved = saveAndCache(url);
            log.info("✅ Generated new short key '{}'", saved.getShortKey());
            return saved;
        }

        throw new IllegalStateException(
                String.format("Unable to generate unique key after %d attempts", MAX_GENERATION_ATTEMPTS)
        );
    }

    private Url saveAndCache(Url url) {
        Url saved = urlRepository.save(url);
        String cacheKey = URL_CACHE_PREFIX + saved.getShortKey();
        cacheUrl(cacheKey, saved);
        return saved;
    }

    private Url buildUrl(String originalUrl, String shortKey, boolean custom, Instant expiresAt) {
        return Url.builder()
                .shortKey(shortKey)
                .originalUrl(originalUrl)
                .customAlias(custom)
                .isActive(true)
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .build();
    }

    private Optional<Url> getFromCache(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof Url) {
                return Optional.of((Url) cached);
            }
        } catch (Exception e) {
            log.warn("Cache read error for key '{}': {}", cacheKey, e.getMessage());
        }
        return Optional.empty();
    }

    private void cacheUrl(String cacheKey, Url url) {
        try {
            redisTemplate.opsForValue().set(
                    cacheKey,
                    url,
                    Duration.ofSeconds(appProperties.getCacheTtlSeconds())
            );
            log.debug("🧠 Cached key '{}' for {}s",
                    url.getShortKey(), appProperties.getCacheTtlSeconds());
        } catch (Exception e) {
            log.warn("Failed to cache key '{}': {}", url.getShortKey(), e.getMessage());
        }
    }

    /**
     * Invalidates the cache for a specific short key
     */
    public void invalidateCache(String shortKey) {
        try {
            String cacheKey = URL_CACHE_PREFIX + shortKey;
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (deleted) {
                log.debug("🗑️ Invalidated cache for key '{}'", shortKey);
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate cache for key '{}': {}", shortKey, e.getMessage());
        }
    }
}