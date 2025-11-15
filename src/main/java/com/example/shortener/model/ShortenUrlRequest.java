package com.example.shortener.model;

import lombok.Data;
import java.time.Instant;

@Data
public class ShortenUrlRequest {
    private String url;
    private String customAlias;
    private Instant expiresAt;
}