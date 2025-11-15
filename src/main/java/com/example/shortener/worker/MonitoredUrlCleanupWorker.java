package com.example.shortener.worker;

import com.example.shortener.entity.Url;
import com.example.shortener.repository.UrlRepository;
import com.example.shortener.worker.metrics.CleanupMetrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoredUrlCleanupWorker {

    private final UrlRepository urlRepository;
    private final CleanupMetrics cleanupMetrics;
    private static final int BATCH_SIZE = 1000;

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    @Transactional
    public void deactivateExpiredUrls() {
        Timer.Sample timer = cleanupMetrics.startTimer();
        Instant now = Instant.now();
        long totalDeactivated = 0;

        try {
            log.info("🕒 Starting expired URL deactivation at {}", now);

            // Use bulk update for better performance on large datasets
            int updatedCount = urlRepository.bulkDeactivateExpiredUrls(now, now);

            if (updatedCount > 0) {
                totalDeactivated = updatedCount;
                cleanupMetrics.recordExpiredUrls(updatedCount);
                log.info("✅ Bulk deactivated {} expired URLs", updatedCount);
            }

        } catch (Exception e) {
            log.error("❌ Error during expired URL deactivation: {}", e.getMessage(), e);
        } finally {
            cleanupMetrics.stopTimer(timer);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void permanentlyDeleteUrls() {
        Timer.Sample timer = cleanupMetrics.startTimer();
        Instant cutoff = Instant.now().minusSeconds(30 * 24 * 3600L);
        long totalDeleted = 0;

        try {
            log.info("🗑️ Starting permanent URL deletion for URLs deleted before {}", cutoff);

            List<Url> oldDeleted;
            int batchNumber = 0;

            do {
                PageRequest pages = PageRequest.of(0, BATCH_SIZE);
                oldDeleted = urlRepository.findSoftDeletedBefore(cutoff, pages);

                if (!oldDeleted.isEmpty()) {
                    batchNumber++;
                    urlRepository.deleteAllInBatch(oldDeleted);
                    totalDeleted += oldDeleted.size();

                    log.debug("🗑️ Batch {}: deleted {} URLs", batchNumber, oldDeleted.size());
                    Thread.sleep(50); // Minimal delay
                }
            } while (!oldDeleted.isEmpty() && oldDeleted.size() == BATCH_SIZE);

            cleanupMetrics.recordPermanentlyDeleted((int) totalDeleted);
            log.info("✅ Permanent deletion completed. Total deleted: {}", totalDeleted);

        } catch (Exception e) {
            log.error("❌ Error during permanent URL deletion: {}", e.getMessage(), e);
        } finally {
            cleanupMetrics.stopTimer(timer);
        }
    }
}