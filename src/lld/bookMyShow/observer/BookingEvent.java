package lld.bookMyShow.observer;

import lld.bookMyShow.model.Booking;
import lld.bookMyShow.model.User;

import java.time.LocalDateTime;

/**
 * Immutable event object published when a booking state changes.
 *
 * Observer Pattern — this is the "event" / "message" passed from
 * Subject (BookingEventPublisher) to Observers (notification listeners).
 *
 * SOLID - SRP: BookingEvent only carries event data — no logic.
 * Immutable: safe to share across multiple listener threads.
 */
public final class BookingEvent {

    private final BookingEventType type;
    private final Booking          booking;
    private final User             user;
    private final LocalDateTime    occurredAt;
    private final String           additionalInfo; // optional: reason for cancel, etc.

    public BookingEvent(BookingEventType type, Booking booking, User user) {
        this(type, booking, user, null);
    }

    public BookingEvent(BookingEventType type, Booking booking, User user, String additionalInfo) {
        this.type           = type;
        this.booking        = booking;
        this.user           = user;
        this.occurredAt     = LocalDateTime.now();
        this.additionalInfo = additionalInfo;
    }

    public BookingEventType getType()           { return type; }
    public Booking          getBooking()        { return booking; }
    public User             getUser()           { return user; }
    public LocalDateTime    getOccurredAt()     { return occurredAt; }
    public String           getAdditionalInfo() { return additionalInfo; }

    @Override
    public String toString() {
        return String.format("BookingEvent{type=%s, bookingId='%s', userId='%s', at=%s}",
                type, booking.getBookingId(), user.getUserId(), occurredAt);
    }
}
