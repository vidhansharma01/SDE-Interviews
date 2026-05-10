package lld.urlShortener.service;

import lld.urlShortener.model.Url;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Core URL Shortener service contract.
 * SOLID - DIP: All callers depend on this abstraction.
 * SOLID - ISP: Lean interface — only URL shortening concerns.
 */
public interface UrlShortenerService {

    /**
     * Shorten a long URL.
     * If same user + same URL already shortened → returns existing code (idempotent).
     *
     * @param longUrl      original URL to shorten
     * @param userId       owner (null = anonymous)
     * @param customAlias  optional custom code, e.g. "my-blog"; null = auto-generate
     * @param expiresAt    optional TTL; null = never expires
     * @return             created (or existing) Url entity
     */
    Url shorten(String longUrl, String userId, String customAlias, Instant expiresAt);

    /**
     * Resolve a short code to its long URL for redirect.
     * Read path: Bloom Filter → LRU Cache → DB.
     *
     * @param shortCode   7-char code
     * @param referrer    HTTP Referer header (for analytics)
     * @param country     GeoIP-resolved country (for analytics)
     * @param userAgent   browser user agent (for analytics)
     * @return            long URL to redirect to
     */
    String resolve(String shortCode, String referrer, String country, String userAgent);

    /**
     * Soft-delete a URL. Only the owner can delete their URL.
     * Short code becomes inaccessible (410 Gone).
     */
    void delete(String shortCode, String userId);

    /** Returns all URLs created by a user, sorted by createdAt DESC. */
    List<Url> listByUser(String userId, int limit, int offset);

    /** Fetch full Url entity — for admin/dashboard use. */
    Optional<Url> getUrl(String shortCode);
}
