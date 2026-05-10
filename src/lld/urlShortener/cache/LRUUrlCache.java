package lld.urlShortener.cache;

import lld.urlShortener.model.Url;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * LRU URL Cache — in-process, thread-safe, with per-entry TTL.
 *
 * ── Why LRU? ─────────────────────────────────────────────────────────
 * URL access follows Zipf's Law (power-law): top 20% of URLs → 80% of traffic.
 * LRU keeps hot URLs in memory and naturally evicts cold ones.
 *
 * ── Implementation ───────────────────────────────────────────────────
 * LinkedHashMap(accessOrder=true) provides LRU semantics out of the box:
 *  - get() moves entry to tail (most recently used)
 *  - removeEldestEntry() evicts head (least recently used) when full
 *  - O(1) for get, put, eviction
 *
 * ── TTL Enforcement (Lazy Expiry) ─────────────────────────────────────
 * No background eviction thread. On get(), check if entry TTL has elapsed.
 * If expired → remove + return empty. LRU pressure also evicts expired entries.
 * This avoids the complexity of a background sweeper for LLD demonstration.
 *
 * ── Thread Safety ────────────────────────────────────────────────────
 * All public methods synchronized. For production: use Caffeine cache library.
 */
public class LRUUrlCache {

    private static final long DEFAULT_TTL_SECONDS = 3600L;  // 1 hour

    private record CacheEntry(Url url, Instant expiresAt) {
        boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    }

    private final int capacity;
    private final LinkedHashMap<String, CacheEntry> map;

    public LRUUrlCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.capacity = capacity;
        this.map = new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > LRUUrlCache.this.capacity;
            }
        };
    }

    /** O(1) get. Returns empty if not cached or TTL has expired (lazy expiry). */
    public synchronized Optional<Url> get(String shortCode) {
        CacheEntry entry = map.get(shortCode);
        if (entry == null)        return Optional.empty();
        if (entry.isExpired()) {
            map.remove(shortCode);
            return Optional.empty();
        }
        return Optional.of(entry.url());
    }

    /** O(1) put. Uses default TTL if ttlSeconds = 0. Evicts LRU entry if full. */
    public synchronized void put(String shortCode, Url url, long ttlSeconds) {
        long effectiveTtl = ttlSeconds > 0 ? ttlSeconds : DEFAULT_TTL_SECONDS;
        map.put(shortCode, new CacheEntry(url, Instant.now().plusSeconds(effectiveTtl)));
    }

    /** O(1) invalidate — called on delete/expire to prevent stale redirects. */
    public synchronized void invalidate(String shortCode) {
        map.remove(shortCode);
    }

    public synchronized int size() { return map.size(); }
}
