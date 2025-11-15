package com.example.shortener.repository;

import com.example.shortener.entity.Url;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UrlRepository extends JpaRepository<Url, UUID> {

    Optional<Url> findByShortKeyAndDeletedAtIsNull(String shortKey);

    boolean existsByShortKey(String shortKey);

    @Query("""
            SELECT u FROM Url u
            WHERE u.shortKey = :shortKey
              AND u.createdAt BETWEEN :from AND :to
            """)
    Optional<Url> findByShortKeyInRange(String shortKey, Instant from, Instant to);

    @Query("""
            SELECT u FROM Url u
            WHERE u.shortKey = :shortKey
              AND u.deletedAt IS NULL
              AND u.createdAt BETWEEN :from AND :to
            """)
    Optional<Url> findActiveByShortKeyInRange(String shortKey, Instant from, Instant to);

    List<Url> findByIsActiveTrueAndExpiresAtBefore(Instant now);

    @Query("""
            SELECT u FROM Url u 
            WHERE u.deletedAt IS NOT NULL 
                AND u.deletedAt < :cutoff 
            ORDER BY u.deletedAt
            """)
    List<Url> findSoftDeletedBefore(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Modifying
    @Query("""
             UPDATE Url u SET u.isActive = false, u.deletedAt = :now 
             WHERE u.isActive = true 
                 AND u.expiresAt < :cutoff
            """)
    int bulkDeactivateExpiredUrls(@Param("cutoff") Instant cutoff, @Param("now") Instant now);
}
