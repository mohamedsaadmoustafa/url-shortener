package com.example.shortener.util;

import com.example.shortener.util.Base62;
import org.springframework.stereotype.Component;

/**
 * Generates short keys from numeric IDs using Base62.
 */
@Component
public class IdBasedKeyGenerator {

    /**
     * Converts a numeric ID to Base62 short key.
     *
     * @param id auto-increment ID from database
     * @return Base62 encoded key
     */
    public String generate(long id) {
        return Base62.encode(id);
    }
}