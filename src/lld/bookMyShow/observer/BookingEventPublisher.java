package lld.bookMyShow.observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Event publisher — the "Subject" in the Observer Pattern.
 *
 * Maintains a registry of BookingEventListeners and notifies all of them
 * when a booking event occurs.
 *
 * SOLID - OCP: New listeners are added via register() — publisher never changes.
 * SOLID - SRP: Publisher only manages listener registry and dispatches events.
 *              It does NOT know what SMS/Email/Push means — that's the listener's job.
 *
 * Thread safety: CopyOnWriteArrayList ensures safe iteration even if
 * listeners are added/removed concurrently (rare, tolerable performance tradeoff).
 *
 * Async consideration: In production, notifications fire on a separate thread pool
 * (ExecutorService) so a slow SMS gateway doesn't block the booking critical path.
 * Kept synchronous here for LLD clarity.
 */
public class BookingEventPublisher {

    // CopyOnWriteArrayList: thread-safe, optimized for read-heavy (notify >> register)
    private final List<BookingEventListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Register a new event listener. Can be called at any time.
     * SOLID - OCP: each new listener registration is an extension, not modification.
     */
    public void register(BookingEventListener listener) {
        listeners.add(listener);
        System.out.printf("[EventPublisher] Registered listener: %s%n",
                listener.getClass().getSimpleName());
    }

    public void unregister(BookingEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Publish an event to all registered listeners.
     *
     * Each listener is called independently — a failure in one listener
     * (e.g., SMS gateway down) must NOT prevent other listeners from running.
     */
    public void publish(BookingEvent event) {
        System.out.printf("[EventPublisher] Publishing event: %s to %d listeners%n",
                event.getType(), listeners.size());

        for (BookingEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                // Isolate listener failures — log and continue
                System.err.printf("[EventPublisher] Listener %s failed for event %s: %s%n",
                        listener.getClass().getSimpleName(), event.getType(), e.getMessage());
            }
        }
    }
}
