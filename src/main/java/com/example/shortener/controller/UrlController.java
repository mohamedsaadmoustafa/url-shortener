package com.example.shortener.controller;

import com.example.shortener.entity.BlacklistUrl;
import com.example.shortener.entity.Url;
import com.example.shortener.model.ShortenUrlRequest;
import com.example.shortener.model.ShortenUrlResponse;
import com.example.shortener.properties.AppProperties;
import com.example.shortener.service.BlacklistService;
import com.example.shortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "URL Shortener", description = "Endpoints for creating and resolving short URLs")
public class UrlController {
    private final UrlService urlService;
    private final AppProperties appProperties;
    private final BlacklistService blacklistService;

    @PostMapping("/shorten")
    @Operation(summary = "Create a short URL")
    public ResponseEntity<ShortenUrlResponse> shorten(@RequestBody ShortenUrlRequest request,
                                                      HttpServletRequest httpRequest) {

        String ip = httpRequest.getRemoteAddr();
        String ua = httpRequest.getHeader("User-Agent");

        Url url = urlService.createShortUrl(
                request.getUrl(),
                request.getCustomAlias(),
                request.getExpiresAt(),
                ip,
                ua
        );

        String baseUrl = System.getenv().getOrDefault("BASE_URL", appProperties.getBaseUrl());
        ShortenUrlResponse response = new ShortenUrlResponse(
                url.getShortKey(),
                String.format("%s/%s", baseUrl, url.getShortKey())
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/blacklist")
    @Operation(summary = "Add a URL pattern to the blacklist")
    public ResponseEntity<BlacklistUrl> addToBlacklist(@RequestParam("pattern") String pattern) {
        BlacklistUrl bl = blacklistService.addPattern(pattern);
        return ResponseEntity.ok(bl);
    }
}