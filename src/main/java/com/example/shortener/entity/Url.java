package com.example.shortener.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a shortened URL entity.
 *
 * This entity stores the mapping between the original URL and the generated short key.
 * Supports custom aliases, expiration time, click count, and soft deletion.
 */
@Entity
@Table(
        name = "urls",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"short_key", "created_at"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Url implements Serializable {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * The creation timestamp is part of the composite primary key in the database,
     * so it must be marked as non-nullable and immutable.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "short_key", nullable = false)
    private String shortKey;

    @Column(name = "original_url", nullable = false, columnDefinition = "text")
    private String originalUrl;

    @Column(name = "custom_alias")
    private boolean customAlias;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "click_count")
    private long clickCount = 0L;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
