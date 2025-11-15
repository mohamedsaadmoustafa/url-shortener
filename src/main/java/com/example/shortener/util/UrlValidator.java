package com.example.shortener.util;

import com.example.shortener.entity.Url;
import com.example.shortener.entity.AbuseEvent;
import com.example.shortener.entity.AbuseEventType;
import com.example.shortener.service.AbuseEventService;
import com.example.shortener.service.BlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class UrlValidator {

    private final BlacklistService blacklistService;
    private final AbuseEventService abuseEventService;

    /**
     * Validates the original URL before creating a short URL.
     * Throws IllegalArgumentException if invalid or blacklisted.
     *
     * @param url the original URL to validate
     * @param ipAddress the IP address of the requester (optional)
     * @param userAgent the User-Agent string (optional)
     */
    public void validate(String url, String ipAddress, String userAgent) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Original URL cannot be empty");
        }

        if (blacklistService.isBlacklisted(url)) {
            // Record an abuse event for attempted blacklisted URL
            AbuseEvent event = AbuseEvent.builder()
                    .shortKey(null) // URL not yet created
                    .eventType(AbuseEventType.BLACKLIST_VIOLATION)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .referer(url) // store the attempted URL
                    .createdAt(Instant.now())
                    .build();

            abuseEventService.recordEvent(event);

            throw new IllegalArgumentException("URL is blacklisted and cannot be shortened");
        }
    }

    /**
     * Checks if the given URL entity is active (not expired and isActive flag is true)
     *
     * @param url the Url entity
     * @return true if active, false otherwise
     */
    public boolean isActive(Url url) {
        return url.isActive() &&
                (url.getExpiresAt() == null || url.getExpiresAt().isAfter(Instant.now()));
    }
}
