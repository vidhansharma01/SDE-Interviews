package lld.bookMyShow.model;

/**
 * Represents the real-time state of a seat for a specific show.
 *
 * State transitions (valid):
 *   AVAILABLE → LOCKED    : user selects seat and initiates payment
 *   LOCKED    → AVAILABLE : lock expires (10-min TTL) or user cancels
 *   LOCKED    → BOOKED    : payment confirmed
 *   BOOKED    → AVAILABLE : user cancels (refund case)
 *   Any       → BLOCKED   : admin marks seat as broken/unavailable
 */
public enum SeatStatus {
    AVAILABLE,
    LOCKED,
    BOOKED,
    BLOCKED
}
