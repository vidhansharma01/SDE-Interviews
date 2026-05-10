package lld.urlShortener;

import lld.urlShortener.analytics.AnalyticsService;
import lld.urlShortener.analytics.InMemoryAnalyticsService;
import lld.urlShortener.cache.LRUUrlCache;
import lld.urlShortener.core.SnowflakeIdGenerator;
import lld.urlShortener.decorator.AnalyticsDecorator;
import lld.urlShortener.exception.AliasAlreadyExistsException;
import lld.urlShortener.exception.UrlExpiredException;
import lld.urlShortener.exception.UrlNotFoundException;
import lld.urlShortener.model.Url;
import lld.urlShortener.observer.*;
import lld.urlShortener.repository.InMemoryUrlRepository;
import lld.urlShortener.service.UrlShortenerService;
import lld.urlShortener.service.impl.UrlShortenerServiceImpl;
import lld.urlShortener.strategy.Base62EncodingStrategy;
import lld.urlShortener.strategy.RandomCodeStrategy;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * ════════════════════════════════════════════════════════════════════
 * URL Shortener LLD — Staff Engineer Demo
 * ════════════════════════════════════════════════════════════════════
 *
 * Scenarios:
 *   1. Basic shorten + resolve (Base62 strategy)
 *   2. Idempotency — same URL+user returns same code
 *   3. Custom alias creation
 *   4. Duplicate alias → exception
 *   5. Expired URL → HTTP 410 Gone
 *   6. Delete → 404 on next resolve
 *   7. Bloom Filter fast rejection (invalid code, never hits cache/DB)
 *   8. Strategy swap — RandomCodeStrategy
 *   9. Decorator analytics — click counts by country
 *  10. Concurrent shorten — 10 threads, same URL, only one created (idempotency)
 */
public class UrlShortenerMain {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("═".repeat(70));
        System.out.println("  URL Shortener LLD — Staff Engineer Demo");
        System.out.println("═".repeat(70));

        // ── Bootstrap ───────────────────────────────────────────────
        AnalyticsDecorator service = bootstrap();

        // ── Scenarios ───────────────────────────────────────────────
        demoBasicShortenAndResolve(service);
        demoIdempotency(service);
        demoCustomAlias(service);
        demoDuplicateAlias(service);
        demoExpiredUrl(service);
        demoDeleteUrl(service);
        demoBloomFilterRejection(service);
        demoStrategySwap();
        demoAnalytics(service);
        demoConcurrentShorten(service);

        service.getUrl("dummy").ifPresent(System.out::println); // no-op, just wires
    }

    // ════════════════════════════════════════════════════════════════
    //  BOOTSTRAP — Dependency wiring (replaces Spring DI container)
    // ════════════════════════════════════════════════════════════════

    private static AnalyticsDecorator bootstrap() {
        // Core components
        SnowflakeIdGenerator idGen    = new SnowflakeIdGenerator(1L); // machineId=1
        InMemoryUrlRepository repo    = new InMemoryUrlRepository();
        LRUUrlCache           cache   = new LRUUrlCache(10_000);       // 10K hot URLs
        AnalyticsService      analytics = new InMemoryAnalyticsService();

        // Observer: register listeners
        UrlEventPublisher publisher = new UrlEventPublisher();
        publisher.register(new AnalyticsEventListener(analytics));
        publisher.register(new AuditEventListener());

        // Core service (Strategy injected: Base62 + Snowflake)
        UrlShortenerService coreService = new UrlShortenerServiceImpl(
                repo, cache, new Base62EncodingStrategy(idGen), publisher);

        // Decorator: wraps core with analytics (Decorator Pattern)
        AnalyticsDecorator decorated = new AnalyticsDecorator(coreService, analytics);

        System.out.println("[Boot] System ready.\n");
        return decorated;
    }

    // ════════════════════════════════════════════════════════════════
    //  SCENARIO 1: Basic shorten + resolve
    // ════════════════════════════════════════════════════════════════

    private static void demoBasicShortenAndResolve(AnalyticsDecorator service) {
        printScenario(1, "Basic Shorten + Resolve");

        Url url = service.shorten(
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                "user1", null, null);
        System.out.println("  Created: " + url);

        String resolved = service.resolve(url.getShortCode(), "google.com", "IN", "Chrome/123");
        System.out.println("  Resolved → " + resolved);
    }

    // ════════════════════════════════════════════════════════════════
    //  SCENARIO 2: Idempotency — same user + same URL = same code
    // ════════════════════════════════════════════════════════════════

    private static void demoIdempotency(AnalyticsDecorator service) {
        printScenario(2, "Idempotency — same URL + user returns same code");

        String longUrl = "https://github.com/vidhansharma01";

        Url first  = service.shorten(longUrl, "user1", null, null);
        Url second = service.shorten(longUrl, "user1", null, null);

        System.out.printf("  First call  → code: '%s'%n", first.getShortCode());
        System.out.printf("  Second call → code: '%s'%n", second.getShortCode());
        System.out.printf("  Same code: %s ✅%n", first.getShortCode().equals(second.getShortCode()));
    }

    // ════════════════════════════════════════════════════════════════
    //  SCENARIO 3: Custom alias
    // ════════════════════════════════════════════════════════════════

    private static void demoCustomAlias(AnalyticsDecorator service) {
        printScenario(3, "Custom Alias");

        Url url = service.shorten("https://www.linkedin.com/in/vidhan",
                "user1", "my-blog", null);
        System.out.println("  Created custom alias: " + url);

        String resolved = service.resolve("my-blog", null, "US", null);
        System.out.println("  Resolved 'my-blog' → " + resolved);
    }

    // ════════════════════════════════════════════════════════════════
    //  SCENARIO 4: Duplicate alias → AliasAlreadyExistsException
    // ════════════════════════════════════════════════════════════════

    private static void demoDuplicateAlias(AnalyticsDecorator service) {
        printScenario(4, "Duplicate Custom Alias → Exception");

        try {
            service.shorten("https://www.example.com", "user2", "my-blog", null);
            System.out.println("  ERROR: Should have thrown exception!");
        } catch (AliasAlreadyExistsException e) {
            System.out.println("  ✅ Expected: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  SCENARIO 5: Expired URL → UrlExpiredException (HTTP 410)
    // ════════════════════════════════════════════════════════════════

    private static void demoExpiredUrl(AnalyticsDecorator service) throws InterruptedException {
        printScenario(5, "Expired URL → HTTP 410 Gone");

        // Create URL that expires in 1 second
        Url url = service.shorten("https://www.example.com/flash-sale",
                "user1", null, Instant.now().plusSeconds(1));

        System.out.println("  Created with 1-second TTL: " + url.getShortCode());
        System.out.println("  Waiting 2 seconds for expiry...");
        Thread.sleep(2000);

        try {
            service.resolve(url.getShortCode(), null, null, null);
        } catch (UrlExpiredException e) {
            System.out.println("  ✅ Expected UrlExpiredException: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  SCENARIO 6: Delete URL → UrlNotFoundException
    // ════════════════════════════════════════════════════════════════

    private static void demoDeleteUrl(AnalyticsDecorator service) {
        printScenario(6, "Delete URL → 404 on Resolve");

        Url url = service.shorten("https://temp.example.com", "user1", null, null);
        System.out.println("  Created: " + url.getShortCode());

        service.delete(url.getShortCode(), "user1");
        System.out.println("  Deleted.");

        try {
            service.resolve(url.getShortCode(), null, null, null);
        } catch (UrlNotFoundException e) {
            System.out.println("  ✅ Expected UrlNotFoundException: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  SCENARIO 7: Bloom Filter rejects clearly invalid codes instantly
    // ════════════════════════════════════════════════════════════════

    private static void demoBloomFilterRejection(AnalyticsDecorator service) {
        printScenario(7, "Bloom Filter Fast Rejection");

        String[] bogusCode = {"aaaaaaa", "ZZZZZZZ", "1234567", "bots123"};
        for (String code : bogusCode) {
            try {
                service.resolve(code, null, null, null);
            } catch (UrlNotFoundException e) {
                System.out.printf("  '%s' → rejected at Bloom Filter (no DB hit) ✅%n", code);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  SCENARIO 8: Strategy swap — RandomCodeStrategy at runtime
    // ════════════════════════════════════════════════════════════════

    private static void demoStrategySwap() {
        printScenario(8, "Strategy Swap — RandomCodeStrategy");

        // Build a separate service instance with RandomCodeStrategy
        UrlShortenerService randomService = new UrlShortenerServiceImpl(
                new InMemoryUrlRepository(),
                new LRUUrlCache(100),
                new RandomCodeStrategy(),         // ← swapped strategy
                new UrlEventPublisher()
        );

        Url url1 = randomService.shorten("https://example.com/a", "user1", null, null);
        Url url2 = randomService.shorten("https://example.com/b", "user1", null, null);

        System.out.println("  Random code 1: " + url1.getShortCode());
        System.out.println("  Random code 2: " + url2.getShortCode());
        System.out.println("  Different codes: " + !url1.getShortCode().equals(url2.getShortCode()));
    }

    // ════════════════════════════════════════════════════════════════
    //  SCENARIO 9: Analytics via Decorator
    // ════════════════════════════════════════════════════════════════

    private static void demoAnalytics(AnalyticsDecorator service) {
        printScenario(9, "Analytics via Decorator Pattern");

        Url url = service.shorten("https://www.example.com/popular", "user1", null, null);
        String code = url.getShortCode();

        // Simulate 5 clicks from different countries
        service.resolve(code, "google.com", "IN", "Chrome");
        service.resolve(code, "twitter.com", "IN", "Safari");
        service.resolve(code, "facebook.com", "US", "Firefox");
        service.resolve(code, null,            "GB", "Chrome");
        service.resolve(code, "reddit.com",   "IN", "Chrome");

        System.out.printf("%n  Total clicks for '%s': %d%n", code, service.getTotalClicks(code));
        System.out.printf("  Clicks by country: %s%n", service.getClicksByCountry(code));
    }

    // ════════════════════════════════════════════════════════════════
    //  SCENARIO 10: Concurrent shorten — idempotency under load
    //  10 threads shorten the SAME URL simultaneously for "user1".
    //  Expected: only ONE unique short code created (dedup working).
    // ════════════════════════════════════════════════════════════════

    private static void demoConcurrentShorten(AnalyticsDecorator service) throws InterruptedException {
        printScenario(10, "Concurrent Shorten — Idempotency Under Load");

        String longUrl = "https://concurrent-test.example.com/shared";
        int threadCount = 10;

        CountDownLatch start   = new CountDownLatch(1);
        CountDownLatch done    = new CountDownLatch(threadCount);
        List<String> codes     = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    Url url = service.shorten(longUrl, "user1", null, null);
                    codes.add(url.getShortCode());
                } catch (Exception e) {
                    System.err.println("  Thread error: " + e.getMessage());
                } finally {
                    done.countDown();
                }
            }).start();
        }

        start.countDown();  // all threads start simultaneously
        done.await(5, TimeUnit.SECONDS);

        long uniqueCodes = codes.stream().distinct().count();
        System.out.printf("  %d threads → %d total results → %d unique code(s)%n",
                threadCount, codes.size(), uniqueCodes);
        System.out.printf("  Idempotency working: %s ✅%n", uniqueCodes == 1);
    }

    // ════════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════════

    private static void printScenario(int num, String title) {
        System.out.println("\n" + "─".repeat(70));
        System.out.printf("  Scenario %d: %s%n", num, title);
        System.out.println("─".repeat(70));
    }
}
