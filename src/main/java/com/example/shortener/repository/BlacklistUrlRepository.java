package com.example.shortener.repository;

import com.example.shortener.entity.BlacklistUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlacklistUrlRepository extends JpaRepository<BlacklistUrl, Long> {

    List<BlacklistUrl> findAll();

    boolean existsByUrlPattern(String urlPattern);
}
