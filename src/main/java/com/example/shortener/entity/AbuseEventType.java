package com.example.shortener.entity;

/**
 * Represents the type of abuse event.
 */
public enum AbuseEventType {
    SPAM,                 // URL is flagged as spam
    RATE_LIMIT_EXCEEDED,   // User exceeded allowed requests
    BLACKLIST_VIOLATION,  // Attempted to use a blacklisted URL
    MALWARE,              // URL contains malicious content
    EXCESSIVE_POSTS, EXCESSIVE_GETS, PHISHING              // URL is suspected phishing
}
