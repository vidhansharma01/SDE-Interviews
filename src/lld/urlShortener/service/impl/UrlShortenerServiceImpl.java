package lld.urlShortener.service.impl;

import lld.urlShortener.cache.LRUUrlCache;
import lld.urlShortener.exception.AliasAlreadyExistsException;
import lld.urlShortener.exception.UrlExpiredException;
import lld.urlShortener.exception.UrlNotFoundException;
import lld.urlShortener.model.Url;
import lld.urlShortener.model.UrlStatus;
import lld.urlShortener.observer.UrlEvent;
import lld.urlShortener.observer.UrlEventPublisher;
import lld.urlShortener.observer.UrlEventType;
import lld.urlShortener.repository.UrlRepository;
import lld.urlShortener.service.UrlShortenerService;
import lld.urlShortener.strategy.EncodingStrategy;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.BitSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Core URL Shortener implementation — the central orchestrator.
 *
 * ═══ Design Patterns Applied ════════════════════════════════════════
 *
 * 1. STRATEGY PATTERN — EncodingStrategy (injected via constructor)
 *    Service generates short codes by calling strategy.generateCode().
 *    Swap Base62EncodingStrategy ↔ RandomCodeStrategy without any change here.
 *
 * 2. OBSERVER PATTERN — UrlEventPublisher (injected via constructor)
 *    After every state change (create/access/delete/expire), publishes a UrlEvent.
 *    Analytics, Audit listeners react independently — service never knows who listens.
 *
 * 3. REPOSITORY PATTERN — UrlRepository (injected via constructor)
 *    Service never touches ConcurrentHashMap. Swap InMemoryUrlRepository
 *    for PostgresUrlRepository with zero changes here. (DIP in action).
 *
 * 4. DECORATOR PATTERN — this class is the "component" being decorated.
 *    AnalyticsDecorator wraps this service to add transparent analytics
 *    without sub-classing or modifying this class (OCP in action).
 *
 * ═══ Read Path Optimization — 3-Layer Lookup ════════════════════════
 *
 * resolve() follows this order:
 *   1. Bloom Filter (local, nanoseconds):
 *      "Definitely absent" → return 404 IMMEDIATELY (no Redis/DB hit).
 *      "Probably present"  → proceed to cache.
 *      This eliminates ALL DB hits from enumeration bots and invalid codes.
 *
 *   2. LRU Cache (in-process memory, microseconds):
 *      Cache hit → return long URL.
 *      Cache miss → proceed to DB.
 *
 *   3. DB (milliseconds):
 *      Authoritative source. On hit → populate cache for next time.
 *      On miss → definitive 404.
 *
 * ═══ SOLID ══════════════════════════════════════════════════════════
 * S: Service orchestrates — fee logic, code gen, cache, analytics all delegated.
 * O: New encoding? New listener? Zero changes here.
 * L: Fully interchangeable via UrlShortenerService interface.
 * I: Depends on UrlRepository (lean), EncodingStrategy (lean), UrlEventPublisher.
 * D: ALL dependencies are interfaces injected via constructor.
 */
public class UrlShortenerServiceImpl implements UrlShortenerService {

    private static final int MAX_COLLISION_RETRIES = 3; // for RandomCodeStrategy

    private final UrlRepository      repository;
    private final LRUUrlCache        cache;
    private final EncodingStrategy   encodingStrategy;
    private final UrlEventPublisher  eventPublisher;

    // ── Bloom Filter (in-process) ─────────────────────────────────────
    // Sized for 10M URLs, 0.1% false positive rate: ~143M bits ≈ 17 MB
    private final BitSet bloomFilter;
    private final int    bloomBits = 143_775_478;
    private final int    bloomHashFunctions = 10;

    public UrlShortenerServiceImpl(UrlRepository repository,
                                    LRUUrlCache cache,
                                    EncodingStrategy encodingStrategy,
                                    UrlEventPublisher eventPublisher) {
        this.repository       = repository;
        this.cache            = cache;
        this.encodingStrategy = encodingStrategy;
        this.eventPublisher   = eventPublisher;
        this.bloomFilter      = new BitSet(bloomBits);
        System.out.printf("[Service] Initialized with strategy: %s%n", encodingStrategy.strategyName());
    }

    // ═══════════════════════════════════════════════════════════════
    //  1. SHORTEN
    // ═══════════════════════════════════════════════════════════════

    @Override
    public Url shorten(String longUrl, String userId, String customAlias, Instant expiresAt) {
        validateUrl(longUrl);

        String hash = md5(longUrl);

        // ── Idempotency: same user + same URL → return existing code ──
        Optional<Url> existing = repository.findByHashAndUser(hash, userId);
        if (existing.isPresent()) {
            System.out.printf("[Service] Duplicate detected. Returning existing code: '%s'%n",
                    existing.get().getShortCode());
            return existing.get();
        }

        // ── Custom alias validation ────────────────────────────────────
        String shortCode;
        if (customAlias != null && !customAlias.isBlank()) {
            if (repository.existsByShortCode(customAlias))
                throw new AliasAlreadyExistsException(customAlias);
            shortCode = customAlias;
            System.out.printf("[Service] Using custom alias: '%s'%n", shortCode);
        } else {
            // ── Strategy Pattern: generate code (Base62 or Random) ─────
            shortCode = generateUnique(longUrl, userId);
        }

        Url url = Url.builder()
                .shortCode(shortCode)
                .longUrl(longUrl)
                .longUrlHash(hash)
                .userId(userId)
                .expiresAt(expiresAt)
                .isCustomAlias(customAlias != null)
                .build();

        repository.save(url);
        cache.put(shortCode, url, computeCacheTtl(expiresAt));
        bloomFilterAdd(shortCode);

        System.out.printf("[Service] Created: '%s' → '%s' (TTL: %s)%n",
                shortCode, truncate(longUrl, 50), expiresAt != null ? expiresAt : "never");

        // ── Observer Pattern: publish URL_CREATED to all listeners ────
        eventPublisher.publish(new UrlEvent(UrlEventType.URL_CREATED, url));

        return url;
    }

    // ═══════════════════════════════════════════════════════════════
    //  2. RESOLVE (critical hot path)
    // ═══════════════════════════════════════════════════════════════

    @Override
    public String resolve(String shortCode, String referrer, String country, String userAgent) {
        // ── Layer 1: Bloom Filter (nanoseconds) ──────────────────────
        if (!bloomFilterMightContain(shortCode)) {
            System.out.printf("[Service] Bloom Filter: '%s' definitely absent → 404%n", shortCode);
            throw new UrlNotFoundException(shortCode);
        }

        // ── Layer 2: LRU Cache (microseconds) ────────────────────────
        Optional<Url> cachedUrl = cache.get(shortCode);
        if (cachedUrl.isPresent()) {
            System.out.printf("[Service] Cache HIT for '%s'%n", shortCode);
            return resolveAccessible(cachedUrl.get(), referrer, country, userAgent);
        }

        // ── Layer 3: DB (milliseconds) ───────────────────────────────
        System.out.printf("[Service] Cache MISS for '%s' — querying DB%n", shortCode);
        Url url = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        // Populate cache for future hits (read-through pattern)
        cache.put(shortCode, url, computeCacheTtl(url.getExpiresAt()));

        return resolveAccessible(url, referrer, country, userAgent);
    }

    /** Validates URL is accessible, increments click count, publishes event. */
    private String resolveAccessible(Url url, String referrer, String country, String userAgent) {
        if (!url.isAccessible()) {
            // Expire lazily on first access after TTL elapses
            if (url.isExpired()) {
                repository.updateStatus(url.getShortCode(), UrlStatus.EXPIRED.name());
                cache.invalidate(url.getShortCode());
                eventPublisher.publish(new UrlEvent(UrlEventType.URL_EXPIRED, url));
                throw new UrlExpiredException(url.getShortCode());
            }
            throw new UrlNotFoundException(url.getShortCode()); // DELETED case
        }

        url.incrementClickCount();

        // ── Observer Pattern: publish URL_ACCESSED (async — never blocks redirect) ──
        eventPublisher.publish(new UrlEvent(UrlEventType.URL_ACCESSED, url, referrer, country, userAgent));

        System.out.printf("[Service] Resolved '%s' → '%s' (total clicks: %d)%n",
                url.getShortCode(), truncate(url.getLongUrl(), 50), url.getClickCount());

        return url.getLongUrl();
    }

    // ═══════════════════════════════════════════════════════════════
    //  3. DELETE
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void delete(String shortCode, String userId) {
        Url url = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (userId != null && !userId.equals(url.getUserId()))
            throw new SecurityException("User '" + userId + "' does not own code '" + shortCode + "'");

        repository.updateStatus(shortCode, UrlStatus.DELETED.name());
        cache.invalidate(shortCode);
        // Bloom Filter: can't remove from standard bloom filter — a false-positive
        // on a deleted URL will hit DB/cache (null result) → tolerable.

        eventPublisher.publish(new UrlEvent(UrlEventType.URL_DELETED, url));
        System.out.printf("[Service] Deleted '%s'%n", shortCode);
    }

    // ═══════════════════════════════════════════════════════════════
    //  4. LIST / GET
    // ═══════════════════════════════════════════════════════════════

    @Override
    public List<Url> listByUser(String userId, int limit, int offset) {
        return repository.findByUserId(userId, limit, offset);
    }

    @Override
    public Optional<Url> getUrl(String shortCode) {
        return repository.findByShortCode(shortCode);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Private helpers
    // ═══════════════════════════════════════════════════════════════

    /**
     * Generates a unique short code, retrying on collision (for RandomCodeStrategy).
     * Base62EncodingStrategy never collides — loop runs exactly once.
     */
    private String generateUnique(String longUrl, String userId) {
        for (int attempt = 1; attempt <= MAX_COLLISION_RETRIES; attempt++) {
            String code = encodingStrategy.generateCode(longUrl, userId);
            if (!repository.existsByShortCode(code)) return code;
            System.out.printf("[Service] Collision on '%s' (attempt %d) — retrying%n", code, attempt);
        }
        throw new RuntimeException("Failed to generate unique code after " + MAX_COLLISION_RETRIES + " retries");
    }

    /** Computes cache TTL respecting the URL's expiresAt. */
    private long computeCacheTtl(Instant expiresAt) {
        if (expiresAt == null) return 3600L; // default: 1 hour cache TTL for eternal URLs
        long ttl = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, Math.min(ttl, 3600L)); // cap at 1 hour
    }

    /** Double-hash Bloom Filter: h_i(x) = |h1 + i*h2| % m */
    private void bloomFilterAdd(String code) {
        int h1 = code.hashCode();
        int h2 = code.chars().reduce(31, (a, c) -> a * 31 + c);
        for (int i = 0; i < bloomHashFunctions; i++) {
            bloomFilter.set(Math.abs((h1 + (long)i * h2) % bloomBits));
        }
    }

    private boolean bloomFilterMightContain(String code) {
        int h1 = code.hashCode();
        int h2 = code.chars().reduce(31, (a, c) -> a * 31 + c);
        for (int i = 0; i < bloomHashFunctions; i++) {
            if (!bloomFilter.get(Math.abs((h1 + (long)i * h2) % bloomBits))) return false;
        }
        return true;
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(md.digest(input.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode()); // fallback
        }
    }

    private void validateUrl(String url) {
        if (url == null || url.isBlank())
            throw new IllegalArgumentException("longUrl must not be blank");
        if (!url.startsWith("http://") && !url.startsWith("https://"))
            throw new IllegalArgumentException("longUrl must start with http:// or https://");
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
