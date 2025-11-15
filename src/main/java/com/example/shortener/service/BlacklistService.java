package com.example.shortener.service;

import com.example.shortener.entity.BlacklistUrl;
import com.example.shortener.repository.BlacklistUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Service to check if a URL is blacklisted (unsafe)
 */
@Service
@RequiredArgsConstructor
public class BlacklistService {

    private final BlacklistUrlRepository blacklistUrlRepository;

    /**
     * Checks if the given URL matches any blacklisted patterns.
     *
     * @param url the URL to check
     * @return true if URL is blacklisted, false otherwise
     */
    public boolean isBlacklisted(String url) {
        List<BlacklistUrl> blacklist = blacklistUrlRepository.findAll();
        return blacklist.stream().anyMatch(b -> url.contains(b.getUrlPattern()));
    }

    /**
     * Add a new URL pattern to the blacklist.
     *
     * @param pattern URL pattern to block
     * @return saved BlacklistUrl entity
     */
    public BlacklistUrl addPattern(String pattern) {
        BlacklistUrl blacklistUrl = BlacklistUrl.builder()
                .urlPattern(pattern)
                .createdAt(Instant.now())
                .build();
        return blacklistUrlRepository.save(blacklistUrl);
    }
}
