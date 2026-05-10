package lld.bookMyShow.observer;

/**
 * Observer interface — any notification handler implements this.
 *
 * Observer Pattern — "Observer" role.
 * SOLID - ISP: Lean interface — only one method, nothing irrelevant.
 * SOLID - DIP: BookingEventPublisher depends on this abstraction,
 *              not on SmsNotificationListener or EmailNotificationListener.
 * SOLID - OCP: To add PushNotificationListener, just create a new class
 *              implementing this interface and register it. Zero changes to publisher.
 */
public interface BookingEventListener {

    /**
     * Called when a booking event occurs.
     * Implementations must NOT throw exceptions — handle internally and log.
     *
     * @param event  the booking event to handle
     */
    void onEvent(BookingEvent event);
}
