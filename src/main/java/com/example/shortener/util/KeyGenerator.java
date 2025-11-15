package com.example.shortener.util;

import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.UUID;
/**
 * Generates unique short keys using Base62 encoding.
 * Uses UUID.randomUUID() for randomness and uniqueness.
 */
@Component
public class KeyGenerator {

    /**
     * Generates a Base62-encoded key using UUID.
     * The result is typically ~22 characters long.
     */
    public String generate() {
        UUID uuid = UUID.randomUUID();
        // Convert UUID to 16-byte array
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return Base62.encode(buffer.array());
    }
}