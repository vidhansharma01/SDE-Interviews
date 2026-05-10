package lld.urlShortener.analytics;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory analytics — for demo and testing.
 *
 * In production: writes to Cassandra / ClickHouse (write-heavy, query by shortCode)
 * and aggregations via Flink / Apache Spark streaming jobs.
 *
 * ── Data Structures ──────────────────────────────────────────────────
 * clickCounts:     shortCode → AtomicLong (thread-safe counter, no lock needed)
 * countryStats:    shortCode → (country → AtomicLong) — nested concurrent map
 * userCodeClicks:  userId → (shortCode → clicks) — for top-URL queries
 *
 * AtomicLong for counters: lock-free CAS operations, no synchronized blocks.
 */
public class InMemoryAnalyticsService implements AnalyticsService {

    private final Map<String, AtomicLong>                clickCounts  = new ConcurrentHashMap<>();
    private final Map<String, Map<String, AtomicLong>>   countryStats = new ConcurrentHashMap<>();
    private final Map<String, Map<String, AtomicLong>>   userCodeClicks = new ConcurrentHashMap<>();

    @Override
    public void recordClick(String shortCode, String userId, String referrer,
                            String country, String userAgent, Instant clickedAt) {
        // Total click counter — AtomicLong.incrementAndGet() is lock-free
        clickCounts.computeIfAbsent(shortCode, k -> new AtomicLong(0)).incrementAndGet();

        // Country breakdown
        if (country != null) {
            countryStats.computeIfAbsent(shortCode, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(country, k -> new AtomicLong(0))
                    .incrementAndGet();
        }

        // User-level tracking for top-URL queries
        if (userId != null) {
            userCodeClicks.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(shortCode, k -> new AtomicLong(0))
                    .incrementAndGet();
        }

        System.out.printf("[Analytics] Click on '%s' | country='%s' | total=%d%n",
                shortCode, country != null ? country : "unknown",
                clickCounts.get(shortCode).get());
    }

    @Override
    public long getTotalClicks(String shortCode) {
        AtomicLong counter = clickCounts.get(shortCode);
        return counter != null ? counter.get() : 0L;
    }

    @Override
    public Map<String, Long> getClicksByCountry(String shortCode) {
        Map<String, AtomicLong> stats = countryStats.get(shortCode);
        if (stats == null) return Collections.emptyMap();
        return stats.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }

    @Override
    public List<String> getTopUrls(String userId, int limit) {
        Map<String, AtomicLong> userStats = userCodeClicks.get(userId);
        if (userStats == null) return Collections.emptyList();
        return userStats.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
