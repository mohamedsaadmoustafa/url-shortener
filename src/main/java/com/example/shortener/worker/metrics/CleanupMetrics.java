package com.example.shortener.worker.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
public class CleanupMetrics {

    private final MeterRegistry meterRegistry;
    private Counter expiredUrlsCounter;
    private Counter permanentlyDeletedCounter;
    private Timer cleanupTimer;

    @PostConstruct
    public void init() {
        expiredUrlsCounter = Counter.builder("url.cleanup.expired")
                .description("Number of expired URLs deactivated")
                .register(meterRegistry);

        permanentlyDeletedCounter = Counter.builder("url.cleanup.permanently_deleted")
                .description("Number of URLs permanently deleted")
                .register(meterRegistry);

        cleanupTimer = Timer.builder("url.cleanup.duration")
                .description("Time spent on cleanup operations")
                .register(meterRegistry);
    }

    public void recordExpiredUrls(int count) {
        expiredUrlsCounter.increment(count);
    }

    public void recordPermanentlyDeleted(int count) {
        permanentlyDeletedCounter.increment(count);
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(cleanupTimer);
    }
}