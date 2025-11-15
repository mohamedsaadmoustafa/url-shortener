package com.example.shortener.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents a blacklisted URL.
 * Any URL in this list cannot be shortened.
 */
@Entity
@Table(name = "blacklist_urls")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlacklistUrl implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url_pattern", nullable = false, unique = true)
    private String urlPattern; // regex or domain patterns

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
