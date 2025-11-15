package com.example.shortener.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {
    private String baseUrl = "http://localhost:8080";
    private long cacheTtlSeconds = 86400;
    private Qr qr = new Qr();

    @Getter
    @Setter
    public static class Qr {
        private int width;
        private int height;
        private int ttlDays;
    }
}
