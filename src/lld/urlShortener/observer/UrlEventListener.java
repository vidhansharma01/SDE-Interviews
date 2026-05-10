package lld.urlShortener.observer;

/**
 * Observer interface — any component that wants to react to URL events.
 *
 * SOLID - ISP: Single method — no unnecessary methods forced on implementors.
 * SOLID - OCP: Add AnalyticsListener, AuditListener, EmailAlertListener etc.
 *              without changing UrlEventPublisher or any existing listener.
 */
public interface UrlEventListener {
    /**
     * Called when a URL event occurs.
     * Implementations MUST NOT throw — catch internally and log.
     */
    void onEvent(UrlEvent event);
}
