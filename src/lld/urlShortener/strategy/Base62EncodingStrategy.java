package lld.urlShortener.strategy;

import lld.urlShortener.core.Base62Encoder;
import lld.urlShortener.core.SnowflakeIdGenerator;

/**
 * Concrete Strategy: Snowflake ID → Base62 encoded short code.
 *
 * This is the PRODUCTION strategy used by bit.ly, TinyURL at scale:
 *  1. Generate a globally unique 64-bit Snowflake ID (guaranteed unique across machines).
 *  2. Base62-encode it to a 7-character URL-safe string.
 *
 * ── Why this is the best strategy for production ─────────────────────
 * - Zero collision risk: Snowflake guarantees 4096 unique IDs per ms per machine.
 * - No DB lookup needed to check uniqueness before insert.
 * - Time-sortable: newer short codes are lexicographically larger (range queries work).
 * - Does NOT use the longUrl content → same long URL can get different short codes
 *   from different machines/times (intentional: avoids cache invalidation complexity).
 *
 * ── SOLID ─────────────────────────────────────────────────────────────
 * SRP: Only generates codes. Encoding details in Base62Encoder, ID in SnowflakeIdGenerator.
 * OCP: Can swap Base62 for Base58 without touching UrlShortenerService.
 */
public class Base62EncodingStrategy implements EncodingStrategy {

    private final SnowflakeIdGenerator idGenerator;
    private final Base62Encoder        encoder;

    public Base62EncodingStrategy(SnowflakeIdGenerator idGenerator) {
        this.idGenerator = idGenerator;
        this.encoder     = Base62Encoder.getInstance(); // Singleton
    }

    @Override
    public String generateCode(String longUrl, String userId) {
        long id = idGenerator.nextId();  // unique 64-bit ID
        String code = encoder.encode(id); // → 7-char Base62 string
        System.out.printf("[Base62Strategy] Generated code '%s' from Snowflake ID %d%n", code, id);
        return code;
    }

    @Override
    public String strategyName() { return "BASE62_SNOWFLAKE"; }
}
