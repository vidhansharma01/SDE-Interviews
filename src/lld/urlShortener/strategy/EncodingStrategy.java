package lld.urlShortener.strategy;

/**
 * Strategy Pattern — Short Code Generation.
 *
 * ── Why Strategy Pattern here? ────────────────────────────────────────
 * There are multiple valid ways to generate a short code:
 *   1. Base62 encoding of a Snowflake ID     → time-sortable, guaranteed unique
 *   2. Random alphanumeric string            → simpler, collision-prone at scale
 *   3. MD5/SHA-hash prefix of the long URL  → deterministic, content-addressed
 *   4. Custom counter per-user              → short and memorable
 *
 * Without Strategy, UrlShortenerService has if-else chains for each approach.
 * With Strategy, the service doesn't know HOW the code is generated — it just
 * calls generateCode() on whichever strategy was injected.
 *
 * SOLID - OCP: Add new strategy (e.g., HashStrategy) → zero changes to service.
 * SOLID - DIP: UrlShortenerServiceImpl depends on this abstraction.
 * SOLID - LSP: All strategies are interchangeable from the service's perspective.
 */
public interface EncodingStrategy {

    /**
     * Generate a unique short code.
     *
     * @param longUrl  the original URL (some strategies use it, some don't)
     * @param userId   the user (some strategies use user-scoped counters)
     * @return         a URL-safe short code (typically 6–8 chars)
     */
    String generateCode(String longUrl, String userId);

    /** Returns the name of this strategy — for logging and observability. */
    String strategyName();
}
