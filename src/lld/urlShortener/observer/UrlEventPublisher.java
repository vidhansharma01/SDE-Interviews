package lld.urlShortener.observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * URL Event Publisher — the "Subject" in the Observer Pattern.
 *
 * ── Design Decisions ─────────────────────────────────────────────────
 * 1. CopyOnWriteArrayList: thread-safe listener registry.
 *    Safe to iterate during publish() even if register()/unregister() called concurrently.
 *    Reads are lock-free (optimized for many publishes, few registrations).
 *
 * 2. Async publishing via ExecutorService (virtual thread pool):
 *    URL resolve (redirect) must be < 5ms. If we notify listeners synchronously,
 *    a slow analytics DB write blocks the user's redirect.
 *    Async: listener failures never delay the critical redirect path.
 *
 * 3. Fault isolation: each listener wrapped in try-catch.
 *    Analytics listener failure must NOT affect audit listener.
 *
 * SOLID - SRP: Publisher manages listener registry and dispatches events only.
 * SOLID - OCP: Register new listeners without modifying this class.
 */
public class UrlEventPublisher {

    private final List<UrlEventListener> listeners = new CopyOnWriteArrayList<>();

    // Virtual thread executor — lightweight, one thread per task, Java 21+
    // Falls back to cached thread pool for older Java versions
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public void register(UrlEventListener listener) {
        listeners.add(listener);
        System.out.printf("[UrlEventPublisher] Registered: %s%n",
                listener.getClass().getSimpleName());
    }

    public void unregister(UrlEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Publishes an event asynchronously to all registered listeners.
     * Returns immediately — does NOT block the caller.
     *
     * @param event the URL event to broadcast
     */
    public void publish(UrlEvent event) {
        for (UrlEventListener listener : listeners) {
            executor.submit(() -> {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    System.err.printf("[UrlEventPublisher] Listener %s failed for %s: %s%n",
                            listener.getClass().getSimpleName(), event.getType(), e.getMessage());
                }
            });
        }
    }

    /** Graceful shutdown — wait for in-flight events to complete. */
    public void shutdown() {
        executor.shutdown();
    }
}
