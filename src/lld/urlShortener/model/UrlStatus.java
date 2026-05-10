package lld.urlShortener.model;

/**
 * Lifecycle states of a shortened URL.
 *
 * State transitions:
 *   ACTIVE  ──▶ EXPIRED   : automatic when expiresAt < now()
 *   ACTIVE  ──▶ DELETED   : user explicit delete
 *   EXPIRED and DELETED are terminal — cannot transition back.
 */
public enum UrlStatus {
    ACTIVE,
    EXPIRED,
    DELETED
}
