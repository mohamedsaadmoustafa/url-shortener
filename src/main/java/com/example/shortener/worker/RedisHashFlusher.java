package com.example.shortener.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHashFlusher {

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ThreadPoolTaskExecutor taskExecutor; // يجب تعريف Bean في SpringConfig

    private static final String HASH_KEY = "clicks";
    private static final int BATCH_SIZE = 1000;

    @Scheduled(fixedRate = 100_000)
    public void flush() {
        log.info("=== [RedisHashFlusher] Starting Redis hash flush... ===");

        Map<Object, Object> allClicks = redisTemplate.opsForHash().entries(HASH_KEY);
        if (allClicks.isEmpty()) {
            log.info("[RedisHashFlusher] No clicks to flush.");
            return;
        }

        List<Map.Entry<String, Long>> batch = new ArrayList<>(BATCH_SIZE);
        AtomicInteger batchCounter = new AtomicInteger(0);
        long totalProcessed = 0L;

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Map.Entry<Object, Object> entry : allClicks.entrySet()) {
            String shortKey = entry.getKey().toString();
            Long count = Long.parseLong(entry.getValue().toString());

            batch.add(Map.entry(shortKey, count));

            if (batch.size() >= BATCH_SIZE) {
                List<Map.Entry<String, Long>> batchCopy = new ArrayList<>(batch);
                int currentBatch = batchCounter.incrementAndGet();
                futures.add(CompletableFuture.runAsync(() -> processBatch(batchCopy, currentBatch), taskExecutor));
                totalProcessed += batch.size();
                batch.clear();
            }
        }

        // Process remaining records
        if (!batch.isEmpty()) {
            List<Map.Entry<String, Long>> batchCopy = new ArrayList<>(batch);
            int currentBatch = batchCounter.incrementAndGet();
            futures.add(CompletableFuture.runAsync(() -> processBatch(batchCopy, currentBatch), taskExecutor));
            totalProcessed += batch.size();
        }

        // Wait for all batches to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Remove processed keys from Redis
        redisTemplate.delete(HASH_KEY);

        log.info("=== [RedisHashFlusher] Flush complete. Total processed: {} records in {} batches ===",
                totalProcessed, batchCounter.get());
    }

    /**
     * Batch update URLs table click_count
     */
    @Transactional
    protected void processBatch(List<Map.Entry<String, Long>> batch, int batchNumber) {
        if (batch.isEmpty()) return;

        String sql = "UPDATE urls SET click_count = click_count + ? WHERE short_key = ?";

        try {
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Map.Entry<String, Long> e = batch.get(i);
                    ps.setLong(1, e.getValue());
                    ps.setString(2, e.getKey());
                }
                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });

            log.info("[Batch {}] Successfully updated {} URLs", batchNumber, batch.size());

        } catch (DataAccessException dae) {
            log.error("[Batch {}] Failed to update DB. Will retry next flush. Cause: {}", batchNumber, dae.getMessage(), dae);
            throw dae;
        }
    }
}
