package com.example.shortener.repository;

import com.example.shortener.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UrlRepository extends JpaRepository<Url, UUID> {
    Optional<Url> findByShortKeyAndDeletedAtIsNull(String shortKey);

    boolean existsByShortKey(String shortKey);

    Optional<Url> findByShortKey(String shortKey);

    @Query("SELECT u FROM Url u WHERE u.shortKey = :shortKey AND u.createdAt BETWEEN :from AND :to")
    Optional<Url> findByShortKeyInRange(String shortKey, Instant from, Instant to);

    /**
     * Optimized query to search within a specific partition range.
     * Useful when table is partitioned by created_at.
     */
    @Query("""
                SELECT u
                FROM Url u
                WHERE u.shortKey = :shortKey
                  AND u.deletedAt IS NULL
                  AND u.createdAt BETWEEN :from AND :to
            """)
    Optional<Url> findActiveByShortKeyInRange(String shortKey, Instant from, Instant to);
}
