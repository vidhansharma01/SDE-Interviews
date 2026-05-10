package lld.urlShortener.analytics;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Analytics Service interface.
 * SOLID - DIP: Listeners and service depend on this abstraction.
 */
public interface AnalyticsService {

    /** Record a single click/redirect event. */
    void recordClick(String shortCode, String userId, String referrer,
                     String country, String userAgent, Instant clickedAt);

    /** Total clicks for a short code. */
    long getTotalClicks(String shortCode);

    /** Click breakdown by country for a short code. */
    Map<String, Long> getClicksByCountry(String shortCode);

    /** Most clicked URLs for a given user. */
    List<String> getTopUrls(String userId, int limit);
}
