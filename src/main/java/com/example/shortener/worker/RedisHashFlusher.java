package com.example.shortener.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHashFlusher {

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ThreadPoolTaskExecutor taskExecutor; // يجب تعريف Bean في SpringConfig

    private static final String HASH_KEY = "clicks";
    private static final int BATCH_SIZE = 500;

    @Scheduled(fixedRate = 30_000)
    public void flushIncrementally() {
        // Use HSCAN for incremental processing
        try (Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.opsForHash()
                .scan(HASH_KEY, ScanOptions.scanOptions().count(BATCH_SIZE).build())) {
            List<Map.Entry<String, Long>> batch = new ArrayList<>();
            while (cursor.hasNext()) {
                Map.Entry<Object, Object> entry = cursor.next();
                batch.add(Map.entry(
                        entry.getKey().toString(),
                        Long.parseLong(entry.getValue().toString())
                ));

                if (batch.size() >= BATCH_SIZE) {
                    processAndDeleteBatch(batch);
                    batch.clear();
                }
            }

            // Process remaining
            if (!batch.isEmpty()) {
                processAndDeleteBatch(batch);
            }
        }
    }

    private void processAndDeleteBatch(List<Map.Entry<String, Long>> batch) {
        // Process batch
        processBatch(batch, 1);

        // Delete processed keys
        String[] keys = batch.stream()
                .map(Map.Entry::getKey)
                .toArray(String[]::new);
        redisTemplate.opsForHash().delete(RedisHashFlusher.HASH_KEY, (Object[]) keys);
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
