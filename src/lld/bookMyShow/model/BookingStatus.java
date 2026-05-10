package lld.bookMyShow.model;

/**
 * Booking lifecycle states — implemented as State Pattern.
 * Each state determines what transitions are allowed.
 *
 * INITIATED   : booking record created, seats being locked
 * LOCKED      : seats held, awaiting payment
 * CONFIRMED   : payment successful, QR generated
 * CANCELLED   : user cancelled or payment timed out
 * EXPIRED     : lock TTL elapsed, seats auto-released
 */
public enum BookingStatus {
    INITIATED,
    LOCKED,
    CONFIRMED,
    CANCELLED,
    EXPIRED
}
