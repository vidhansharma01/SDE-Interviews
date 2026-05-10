package lld.urlShortener.observer;

/**
 * Audit Listener — immutable audit log of all URL lifecycle events.
 *
 * In production: writes to an append-only audit table in PostgreSQL,
 * or streams to an immutable event store (Kafka → S3).
 *
 * Reacts to ALL event types — audit trail must be complete.
 * SOLID - SRP: Only writes audit records. No business logic.
 */
public class AuditEventListener implements UrlEventListener {

    @Override
    public void onEvent(UrlEvent event) {
        // In production: persist to audit_log table (append-only)
        System.out.printf("[AUDIT] %s | code='%s' | userId='%s' | at=%s%n",
                event.getType(),
                event.getUrl().getShortCode(),
                event.getUrl().getUserId() != null ? event.getUrl().getUserId() : "anonymous",
                event.getOccurredAt());
    }
}
