package com.example.shortener.service;

import com.example.shortener.properties.AppProperties;
import com.example.shortener.entity.Url;
import com.example.shortener.repository.UrlRepository;
import com.example.shortener.util.UrlValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Service responsible for resolving short URLs.
 *
 * This service:
 * - Looks up URLs in Redis cache.
 * - Falls back to the database if not cached.
 * - Caches active URLs after fetching them.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UrlResolutionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UrlRepository urlRepository;
    private final AppProperties appProperties;
    private final UrlValidator urlValidator;

    /**
     * Resolves the given short key to its corresponding URL entity.
     *
     * @param shortKey the short key to resolve
     * @return an Optional containing the resolved {@link Url}, or empty if not found or inactive
     */
    public Optional<Url> resolve(String shortKey) {
        return resolveFromCache(shortKey).or(() -> resolveFromDatabase(shortKey).map(this::cacheIfActive));
    }

    private Optional<Url> resolveFromCache(String shortKey) {
        String cacheKey = "u:" + shortKey;
        Object cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached instanceof Url url && urlValidator.isActive(url)) {
            log.debug("✅ Cache hit for key '{}'", shortKey);
            return Optional.of(url);
        }
        log.debug("❌ Cache miss or inactive URL for '{}'", shortKey);
        return Optional.empty();
    }

    private Optional<Url> resolveFromDatabase(String shortKey) {
        log.debug("Querying DB for key '{}'", shortKey);
        return urlRepository.findByShortKeyAndDeletedAtIsNull(shortKey).filter(urlValidator::isActive).map(url -> {
            log.debug("✅ Found active URL in DB for '{}'", shortKey);
            return url;
        });
    }

    private Url cacheIfActive(Url url) {
        if (!urlValidator.isActive(url)) return url;

        String cacheKey = "u:" + url.getShortKey();
        redisTemplate.opsForValue().set(cacheKey, url, Duration.ofSeconds(appProperties.getCacheTtlSeconds()));
        log.debug("🧠 Cached '{}' for {}s", url.getShortKey(), appProperties.getCacheTtlSeconds());
        return url;
    }
}
