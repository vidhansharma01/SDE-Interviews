package lld.bookMyShow.observer;

/** Types of booking events published to listeners. */
public enum BookingEventType {
    BOOKING_LOCKED,       // seats held, payment initiated
    BOOKING_CONFIRMED,    // payment success, QR generated
    BOOKING_CANCELLED,    // user cancelled or payment timed out
    BOOKING_EXPIRED       // lock TTL elapsed
}
