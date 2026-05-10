package lld.urlShortener.model;

import java.time.Instant;

/**
 * Core domain entity — the shortened URL mapping.
 *
 * ── Design Decisions ─────────────────────────────────────────────────
 * 1. Builder Pattern: avoids telescoping constructors; makes construction
 *    readable regardless of how many optional fields exist.
 * 2. Immutable core fields (shortCode, longUrl, userId, createdAt):
 *    set once at creation, never changed. Safe to cache across threads.
 * 3. Mutable operational fields (status, clickCount): updated by the service.
 *    volatile status ensures visibility across threads without full synchronization.
 * 4. longUrlHash: MD5(longUrl) stored for O(1) duplicate detection.
 *    Without this, dedup requires a full-string scan of the longUrl index.
 *
 * ── SOLID ────────────────────────────────────────────────────────────
 * SRP: Url holds data and basic derived behaviour. No persistence or network logic.
 */
public final class Url {

    // ── Immutable fields ──────────────────────────────────────────────
    private final String  shortCode;     // "aBcD3f7" — 7-char Base62 or custom alias
    private final String  longUrl;       // "https://www.example.com/very/long/path"
    private final String  longUrlHash;   // MD5(longUrl) — used for duplicate detection
    private final String  userId;        // owner (null = anonymous)
    private final Instant createdAt;
    private final Instant expiresAt;     // null = never expires
    private final boolean isCustomAlias; // true if user provided their own short code

    // ── Mutable operational fields ────────────────────────────────────
    private volatile UrlStatus status;
    private volatile long      clickCount;

    private Url(Builder builder) {
        this.shortCode    = builder.shortCode;
        this.longUrl      = builder.longUrl;
        this.longUrlHash  = builder.longUrlHash;
        this.userId       = builder.userId;
        this.createdAt    = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.expiresAt    = builder.expiresAt;
        this.isCustomAlias = builder.isCustomAlias;
        this.status       = UrlStatus.ACTIVE;
        this.clickCount   = 0;
    }

    // ── Accessors ─────────────────────────────────────────────────────
    public String    getShortCode()    { return shortCode; }
    public String    getLongUrl()      { return longUrl; }
    public String    getLongUrlHash()  { return longUrlHash; }
    public String    getUserId()       { return userId; }
    public Instant   getCreatedAt()    { return createdAt; }
    public Instant   getExpiresAt()    { return expiresAt; }
    public boolean   isCustomAlias()   { return isCustomAlias; }
    public UrlStatus getStatus()       { return status; }
    public long      getClickCount()   { return clickCount; }

    // ── Derived behaviour ─────────────────────────────────────────────

    /** True if the expiry time has passed. Status is NOT automatically updated. */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * True if the URL can be resolved for redirect.
     * Both status must be ACTIVE AND TTL not elapsed.
     */
    public boolean isAccessible() {
        return status == UrlStatus.ACTIVE && !isExpired();
    }

    public synchronized void markExpired() {
        this.status = UrlStatus.EXPIRED;
    }

    public synchronized void markDeleted() {
        this.status = UrlStatus.DELETED;
    }

    public synchronized void incrementClickCount() {
        this.clickCount++;
    }

    @Override
    public String toString() {
        return String.format("Url{code='%s', status=%s, clicks=%d, ttl=%s}",
                shortCode, status, clickCount,
                expiresAt != null ? expiresAt.toString() : "never");
    }

    // ── Builder ───────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String  shortCode;
        private String  longUrl;
        private String  longUrlHash;
        private String  userId;
        private Instant createdAt;
        private Instant expiresAt;
        private boolean isCustomAlias = false;

        public Builder shortCode(String code)      { this.shortCode = code;         return this; }
        public Builder longUrl(String url)         { this.longUrl = url;            return this; }
        public Builder longUrlHash(String hash)    { this.longUrlHash = hash;       return this; }
        public Builder userId(String id)           { this.userId = id;              return this; }
        public Builder createdAt(Instant t)        { this.createdAt = t;            return this; }
        public Builder expiresAt(Instant t)        { this.expiresAt = t;            return this; }
        public Builder isCustomAlias(boolean flag) { this.isCustomAlias = flag;     return this; }

        public Url build() {
            if (shortCode == null || shortCode.isBlank())
                throw new IllegalStateException("shortCode is required");
            if (longUrl == null || longUrl.isBlank())
                throw new IllegalStateException("longUrl is required");
            if (longUrlHash == null || longUrlHash.isBlank())
                throw new IllegalStateException("longUrlHash is required");
            return new Url(this);
        }
    }
}
