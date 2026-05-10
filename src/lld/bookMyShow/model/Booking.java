package lld.bookMyShow.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Core booking entity — the central aggregate in this system.
 *
 * Design Patterns:
 *  - Builder Pattern: Clean construction of complex booking object.
 *  - State Pattern: status field drives what transitions are allowed.
 *    State transition logic is kept in BookingService (not here),
 *    keeping Booking as a plain data-holder (SRP).
 *
 * SOLID - SRP: Booking holds booking data and exposes state helpers.
 *              It does NOT process payments or send notifications.
 *
 * SOLID - OCP: New fields (foodOrders, insuranceId) added via Builder
 *              without touching existing construction code.
 *
 * Immutable fields (set once at creation):
 *   bookingId, userId, showId, seats, amounts, lockedAt
 *
 * Mutable fields (updated by BookingService as state progresses):
 *   status, paymentId, confirmedAt, cancelledAt, qrCode
 */
public class Booking {

    // Immutable
    private final String          bookingId;
    private final String          userId;
    private final String          showId;
    private final List<String>    seatIds;        // e.g. ["A1", "A2"]
    private final double          subtotal;
    private final double          convenienceFee;
    private final double          totalAmount;
    private final LocalDateTime   lockedAt;

    // Mutable — updated as booking progresses through its lifecycle
    private BookingStatus   status;
    private String          paymentId;
    private String          qrCode;
    private LocalDateTime   confirmedAt;
    private LocalDateTime   cancelledAt;
    private String          cancellationReason;

    private Booking(Builder builder) {
        this.bookingId       = builder.bookingId;
        this.userId          = builder.userId;
        this.showId          = builder.showId;
        this.seatIds         = List.copyOf(builder.seatIds);
        this.subtotal        = builder.subtotal;
        this.convenienceFee  = builder.convenienceFee;
        this.totalAmount     = builder.subtotal + builder.convenienceFee;
        this.lockedAt        = LocalDateTime.now();
        this.status          = BookingStatus.LOCKED;
    }

    // ── Accessors ────────────────────────────────────────────────────

    public String        getBookingId()          { return bookingId; }
    public String        getUserId()             { return userId; }
    public String        getShowId()             { return showId; }
    public List<String>  getSeatIds()            { return seatIds; }
    public double        getSubtotal()           { return subtotal; }
    public double        getConvenienceFee()     { return convenienceFee; }
    public double        getTotalAmount()        { return totalAmount; }
    public LocalDateTime getLockedAt()           { return lockedAt; }
    public BookingStatus getStatus()             { return status; }
    public String        getPaymentId()          { return paymentId; }
    public String        getQrCode()             { return qrCode; }
    public LocalDateTime getConfirmedAt()        { return confirmedAt; }
    public LocalDateTime getCancelledAt()        { return cancelledAt; }
    public String        getCancellationReason() { return cancellationReason; }

    // ── State transition helpers (called by BookingService) ──────────

    /**
     * Confirms the booking after successful payment.
     * SOLID - SRP: BookingService decides WHEN to confirm; Booking just records the fact.
     */
    public void confirm(String paymentId, String qrCode) {
        if (this.status != BookingStatus.LOCKED)
            throw new IllegalStateException("Can only confirm a LOCKED booking. Current: " + status);
        this.status      = BookingStatus.CONFIRMED;
        this.paymentId   = paymentId;
        this.qrCode      = qrCode;
        this.confirmedAt = LocalDateTime.now();
    }

    /**
     * Cancels the booking. Allowed from LOCKED or CONFIRMED state.
     */
    public void cancel(String reason) {
        if (this.status == BookingStatus.CANCELLED || this.status == BookingStatus.EXPIRED)
            throw new IllegalStateException("Booking already finalized. Status: " + status);
        this.status             = BookingStatus.CANCELLED;
        this.cancelledAt        = LocalDateTime.now();
        this.cancellationReason = reason;
    }

    /**
     * Marks the booking as expired (lock TTL elapsed, payment not completed).
     */
    public void expire() {
        if (this.status != BookingStatus.LOCKED)
            throw new IllegalStateException("Can only expire a LOCKED booking. Current: " + status);
        this.status = BookingStatus.EXPIRED;
    }

    public boolean isActive() {
        return status == BookingStatus.LOCKED || status == BookingStatus.CONFIRMED;
    }

    @Override
    public String toString() {
        return String.format("Booking{id='%s', user='%s', show='%s', seats=%s, amount=₹%.0f, status=%s}",
                bookingId, userId, showId, seatIds, totalAmount, status);
    }

    // ── Builder ──────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String       bookingId;
        private String       userId;
        private String       showId;
        private List<String> seatIds;
        private double       subtotal;
        private double       convenienceFee = 0;

        public Builder bookingId(String id)           { this.bookingId = id;          return this; }
        public Builder userId(String id)              { this.userId = id;             return this; }
        public Builder showId(String id)              { this.showId = id;             return this; }
        public Builder seatIds(List<String> ids)      { this.seatIds = ids;           return this; }
        public Builder subtotal(double amount)        { this.subtotal = amount;       return this; }
        public Builder convenienceFee(double fee)     { this.convenienceFee = fee;    return this; }

        public Booking build() {
            if (bookingId == null || userId == null || showId == null || seatIds == null || seatIds.isEmpty())
                throw new IllegalStateException("bookingId, userId, showId, seatIds are required");
            return new Booking(this);
        }
    }
}
