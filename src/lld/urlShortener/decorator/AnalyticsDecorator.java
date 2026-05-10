package lld.urlShortener.decorator;

import lld.urlShortener.analytics.AnalyticsService;
import lld.urlShortener.model.Url;
import lld.urlShortener.service.UrlShortenerService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Decorator Pattern — wraps UrlShortenerService with analytics reporting.
 *
 * ── Why Decorator over Inheritance? ─────────────────────────────────
 * If we sub-classed UrlShortenerServiceImpl, we'd be tightly coupled
 * to that specific implementation. If the impl changes, our subclass breaks.
 *
 * The Decorator:
 *  1. Holds a reference to UrlShortenerService (any implementation).
 *  2. Delegates all calls to the wrapped service.
 *  3. After each call, adds its own behaviour (analytics reporting).
 *  4. Transparent to callers — they see a UrlShortenerService, not a decorator.
 *
 * ── SOLID ────────────────────────────────────────────────────────────
 * OCP: UrlShortenerServiceImpl is never modified to add analytics — we wrap it.
 * SRP: UrlShortenerServiceImpl = business logic. AnalyticsDecorator = reporting.
 * LSP: Fully substitutable for UrlShortenerService anywhere it's used.
 * DIP: Both implement UrlShortenerService — callers don't know which one they have.
 *
 * ── Stacking Decorators ───────────────────────────────────────────────
 * Decorators compose. You can stack:
 *   UrlShortenerService service = new UrlShortenerServiceImpl(...);
 *   service = new AnalyticsDecorator(service, analytics);
 *   service = new RateLimitDecorator(service, rateLimiter);
 *   → Every call goes through RateLimit → Analytics → Core service
 */
public class AnalyticsDecorator implements UrlShortenerService {

    private final UrlShortenerService delegate;         // wrapped service
    private final AnalyticsService    analyticsService;

    public AnalyticsDecorator(UrlShortenerService delegate, AnalyticsService analyticsService) {
        this.delegate         = delegate;
        this.analyticsService = analyticsService;
    }

    @Override
    public Url shorten(String longUrl, String userId, String customAlias, Instant expiresAt) {
        Url result = delegate.shorten(longUrl, userId, customAlias, expiresAt);
        // Post-create analytics: track creation per user (for top-creator dashboards)
        System.out.printf("[AnalyticsDecorator] URL created by '%s': code='%s'%n",
                userId != null ? userId : "anonymous", result.getShortCode());
        return result;
    }

    @Override
    public String resolve(String shortCode, String referrer, String country, String userAgent) {
        long start = System.currentTimeMillis();
        String longUrl = delegate.resolve(shortCode, referrer, country, userAgent);
        long latency = System.currentTimeMillis() - start;

        // ── Analytics: record click with latency metric ────────────────
        analyticsService.recordClick(shortCode, null, referrer, country, userAgent, Instant.now());

        System.out.printf("[AnalyticsDecorator] Resolved '%s' in %dms%n", shortCode, latency);
        return longUrl;
    }

    @Override
    public void delete(String shortCode, String userId) {
        delegate.delete(shortCode, userId);
        System.out.printf("[AnalyticsDecorator] Deletion recorded for '%s'%n", shortCode);
    }

    @Override
    public List<Url> listByUser(String userId, int limit, int offset) {
        return delegate.listByUser(userId, limit, offset);
    }

    @Override
    public Optional<Url> getUrl(String shortCode) {
        return delegate.getUrl(shortCode);
    }

    // ── Bonus: analytics-specific queries (exposed as additional methods) ─

    public long getTotalClicks(String shortCode) {
        return analyticsService.getTotalClicks(shortCode);
    }

    public Map<String, Long> getClicksByCountry(String shortCode) {
        return analyticsService.getClicksByCountry(shortCode);
    }

    public List<String> getTopUrls(String userId, int limit) {
        return analyticsService.getTopUrls(userId, limit);
    }
}
