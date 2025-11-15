package com.example.shortener.service;

import com.example.shortener.properties.AppProperties;
import com.example.shortener.entity.Url;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrCodeService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AppProperties appProperties;
    private final UrlService urlService;
    private static final String QR_CACHE_PREFIX = "qr:";

    /**
     * Get QR code PNG bytes for a given short key.
     * Checks Redis first, generates if missing, and caches the result.
     */
    public byte[] getQrCode(String shortKey) throws Exception {
        String qrCacheKey = QR_CACHE_PREFIX + shortKey;

        // 1️⃣ Try to get QR from Redis with proper type handling
        Object cached = redisTemplate.opsForValue().get(qrCacheKey);
        if (cached != null) {
            return convertToByteArray(cached);
        }

        // 2️⃣ Get URL from Redis or DB
        Url url = urlService.getByShortKey(shortKey)
                .orElseThrow(() -> new RuntimeException("Short URL not found: " + shortKey));

        // 3️⃣ Build full URL
        String fullUrl = appProperties.getBaseUrl() + "/" + shortKey;

        // 4️⃣ Generate QR code
        byte[] qrBytes = generateQr(fullUrl, appProperties.getQr().getWidth(), appProperties.getQr().getHeight());

        // 5️⃣ Cache QR code in Redis with proper serialization
        cacheQrCode(qrCacheKey, qrBytes);

        return qrBytes;
    }

    /**
     * Safely convert cached object to byte array
     */
    private byte[] convertToByteArray(Object cached) {
        if (cached instanceof byte[]) {
            log.debug("🚀 Loaded QR from Redis as byte[]");
            return (byte[]) cached;
        } else if (cached instanceof String) {
            log.debug("🔄 Converting cached QR from String to byte[]");
            return ((String) cached).getBytes();
        } else {
            log.warn("❓ Unexpected cache type: {}, regenerating QR", cached.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * Cache QR code with proper serialization
     */
    private void cacheQrCode(String cacheKey, byte[] qrBytes) {
        try {
            redisTemplate.opsForValue().set(
                    cacheKey,
                    qrBytes,
                    Duration.ofDays(appProperties.getQr().getTtlDays())
            );
            log.debug("✅ Cached QR code for key '{}' (TTL: {} days)",
                    cacheKey.substring(QR_CACHE_PREFIX.length()),
                    appProperties.getQr().getTtlDays());
        } catch (Exception e) {
            log.warn("⚠ Failed to cache QR code: {}", e.getMessage());
        }
    }

    private byte[] generateQr(String url, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = Map.of(
                EncodeHintType.MARGIN, 1
        );

        BitMatrix matrix = new MultiFormatWriter()
                .encode(url, BarcodeFormat.QR_CODE, width, height, hints);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    /**
     * Invalidate QR code cache for a short key
     */
    public void invalidateQrCache(String shortKey) {
        try {
            String cacheKey = QR_CACHE_PREFIX + shortKey;
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (deleted) {
                log.debug("🗑️ Invalidated QR cache for key '{}'", shortKey);
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate QR cache for key '{}': {}", shortKey, e.getMessage());
        }
    }
}