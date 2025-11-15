package com.example.shortener.repository;

import com.example.shortener.entity.AbuseEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AbuseEventRepository extends JpaRepository<AbuseEvent, UUID> {
    List<AbuseEvent> findByShortKey(String shortKey);
}
