package lld.urlShortener.observer;

import lld.urlShortener.model.Url;

import java.time.Instant;

/**
 * Immutable event data object published when a URL state changes.
 *
 * Observer Pattern — the "event/message" passed from Subject → Observers.
 * Immutable: zero risk of observers mutating shared state.
 *
 * Optional metadata fields (referrer, country, userAgent) are populated
 * only for URL_ACCESSED events — for click analytics.
 */
public final class UrlEvent {

    private final UrlEventType type;
    private final Url          url;
    private final Instant      occurredAt;
    // Click metadata — populated for URL_ACCESSED events
    private final String       referrer;
    private final String       country;
    private final String       userAgent;

    /** Constructor for non-click events (CREATED, DELETED, EXPIRED). */
    public UrlEvent(UrlEventType type, Url url) {
        this(type, url, null, null, null);
    }

    /** Constructor for URL_ACCESSED events with click metadata. */
    public UrlEvent(UrlEventType type, Url url, String referrer, String country, String userAgent) {
        this.type       = type;
        this.url        = url;
        this.occurredAt = Instant.now();
        this.referrer   = referrer;
        this.country    = country;
        this.userAgent  = userAgent;
    }

    public UrlEventType getType()      { return type; }
    public Url          getUrl()       { return url; }
    public Instant      getOccurredAt(){ return occurredAt; }
    public String       getReferrer()  { return referrer; }
    public String       getCountry()   { return country; }
    public String       getUserAgent() { return userAgent; }

    @Override
    public String toString() {
        return String.format("UrlEvent{type=%s, code='%s', at=%s}", type, url.getShortCode(), occurredAt);
    }
}
