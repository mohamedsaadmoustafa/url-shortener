package com.example.shortener.controller;

import com.example.shortener.entity.Url;
import com.example.shortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;

@Controller
@RequiredArgsConstructor
@Slf4j
@Tag(name = "URL Shortener", description = "Endpoints for creating and resolving short URLs")
public class RedirectController {

    private final UrlService urlService;

    @Operation(summary = "Resolve a short key to full URL")
    @GetMapping("/{key}")
    public ResponseEntity<?> redirect(@PathVariable("key") String key, HttpServletRequest req) {
        log.info("Redirect request received for short key '{}'", key);

        var opt = urlService.resolve(key);
        if (opt.isEmpty()) {
            log.warn("Short key '{}' not found or inactive/expired", key);
            return ResponseEntity.notFound().build();
        }

        Url u = opt.get();
        String ip = req.getRemoteAddr();
        String ua = req.getHeader("User-Agent");
        String referer = req.getHeader("Referer");

        log.info("Short key '{}' resolved to '{}'", key, u.getOriginalUrl());
        log.debug("Click details: IP='{}', User-Agent='{}', Referer='{}'", ip, ua, referer);

        urlService.publishClickEvent(key, ip, ua, referer);
        log.debug("Click event published for key '{}'", key);

        return ResponseEntity.status(302).location(URI.create(u.getOriginalUrl())).build();
    }
}
