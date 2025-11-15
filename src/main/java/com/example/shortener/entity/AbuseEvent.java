package com.example.shortener.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents an abuse detection event for a specific URL or user action.
 */
@Entity
@Table(name = "abuse_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbuseEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "short_key", nullable = false)
    private String shortKey; // URL being abused

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AbuseEventType eventType;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress; // stored as INET in PostgreSQL

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "referer")
    private String referer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
