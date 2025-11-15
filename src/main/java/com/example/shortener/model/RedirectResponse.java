package com.example.shortener.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RedirectResponse {
    private String originalUrl;
    private boolean active;
}