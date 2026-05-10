package lld.urlShortener.observer;

/** Event types published when URL state changes. */
public enum UrlEventType {
    URL_CREATED,    // new short URL created
    URL_ACCESSED,   // redirect happened (click event)
    URL_DELETED,    // user soft-deleted the URL
    URL_EXPIRED     // TTL elapsed — URL deactivated
}
