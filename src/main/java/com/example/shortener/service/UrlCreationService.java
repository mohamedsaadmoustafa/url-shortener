package com.example.shortener.service;

import com.example.shortener.properties.AppProperties;
import com.example.shortener.entity.Url;
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

/**
 * Service responsible for creating short URLs.
 *
 * This service:
 * - Validates input URLs.
 * - Generates or accepts custom aliases.
 * - Persists data to the database.
 * - Caches URLs in Redis for fast access.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UrlCreationService {

    private final UrlRepository urlRepository;
    private final KeyGenerator keyGenerator;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AppProperties appProperties;
    private final UrlValidator urlValidator;

    /**
     * Creates a short URL from the given original URL.
     * Optionally supports a custom alias and expiration time.
     *
     * @param originalUrl the original long URL
     * @param customAlias optional custom short key
     * @param expiresAt   optional expiration timestamp
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

    private Url createCustomAlias(String originalUrl, String alias, Instant expiresAt) {
        if (urlRepository.existsByShortKey(alias)) {
            log.warn("⚠ Custom alias '{}' already exists", alias);
            throw new IllegalArgumentException("Alias already in use");
        }

        Url url = buildUrl(originalUrl, alias, true, expiresAt);
        Url saved = saveAndCache(url);
        log.info("✅ Saved custom alias '{}'", saved.getShortKey());
        return saved;
    }

    private Url createGeneratedAlias(String originalUrl, Instant expiresAt) {
        for (int i = 1; i <= 5; i++) {
            String key = keyGenerator.generate();
            if (urlRepository.existsByShortKey(key)) {
                log.debug("Attempt {}/5: Key '{}' already exists, retrying...", i, key);
                continue;
            }

            Url url = buildUrl(originalUrl, key, false, expiresAt);
            Url saved = saveAndCache(url);
            log.info("✅ Generated new short key '{}'", saved.getShortKey());
            return saved;
        }
        throw new IllegalStateException("Unable to generate unique key after 5 attempts");
    }

    private Url saveAndCache(Url url) {
        Url saved = urlRepository.save(url);
        String cacheKey = "u:" + saved.getShortKey();
        redisTemplate.opsForValue().set(cacheKey, saved, Duration.ofSeconds(appProperties.getCacheTtlSeconds()));
        log.debug("🧠 Cached key '{}' (TTL={}s)", saved.getShortKey(), appProperties.getCacheTtlSeconds());
        return saved;
    }

    private Url buildUrl(String originalUrl, String shortKey, boolean custom, Instant expiresAt) {
        return Url.builder().shortKey(shortKey).originalUrl(originalUrl).customAlias(custom).isActive(true).createdAt(Instant.now()).expiresAt(expiresAt).build();
    }
}
