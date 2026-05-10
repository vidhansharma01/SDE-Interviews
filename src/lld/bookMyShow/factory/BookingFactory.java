package lld.bookMyShow.factory;

import lld.bookMyShow.model.Booking;
import lld.bookMyShow.model.Seat;
import lld.bookMyShow.model.Show;

import java.util.List;
import java.util.UUID;

/**
 * Factory Pattern — BookingFactory.
 *
 * SOLID - SRP: Booking creation logic is isolated here.
 *   BookingService calls the factory — it does not know HOW a Booking is constructed.
 *   Price calculation, convenience fee logic, ID generation all live here.
 *
 * SOLID - OCP: Change how bookings are built (new fee structure, ID format)
 *   by modifying only this class. BookingService never changes.
 *
 * Factory vs Builder:
 *   Builder = step-by-step construction of a complex object (client controls steps).
 *   Factory = encapsulates creation logic + decides internals (factory controls steps).
 *   We use BOTH: Factory decides WHAT to build; Builder is HOW Booking is assembled.
 */
public class BookingFactory {

    private static final double CONVENIENCE_FEE_PERCENT = 0.05; // 5% convenience fee
    private static final double MIN_CONVENIENCE_FEE     = 20.0; // minimum ₹20
    private static final double MAX_CONVENIENCE_FEE     = 100.0; // capped at ₹100

    /**
     * Creates a Booking for the given user, show, and selected seats.
     *
     * @param userId   the user booking the seats
     * @param show     the show being booked
     * @param seats    the specific seat objects selected (already validated as LOCKED)
     * @return         fully constructed Booking entity, ready to be persisted
     */
    public Booking createBooking(String userId, Show show, List<Seat> seats) {
        String bookingId  = generateBookingId();
        double subtotal   = calculateSubtotal(seats);
        double convFee    = calculateConvenienceFee(subtotal);

        System.out.printf("[BookingFactory] Creating booking %s | Seats: %d | Subtotal: ₹%.0f | Fee: ₹%.0f%n",
                bookingId, seats.size(), subtotal, convFee);

        return Booking.builder()
                .bookingId(bookingId)
                .userId(userId)
                .showId(show.getShowId())
                .seatIds(seats.stream().map(Seat::getSeatId).toList())
                .subtotal(subtotal)
                .convenienceFee(convFee)
                .build();
    }

    /** Calculates total seat price by summing individual seat prices. */
    private double calculateSubtotal(List<Seat> seats) {
        return seats.stream().mapToDouble(Seat::getPrice).sum();
    }

    /**
     * Tiered convenience fee:
     *  - 5% of subtotal
     *  - Floor: ₹20 (never below this)
     *  - Cap: ₹100 (never above this)
     */
    private double calculateConvenienceFee(double subtotal) {
        double fee = subtotal * CONVENIENCE_FEE_PERCENT;
        return Math.max(MIN_CONVENIENCE_FEE, Math.min(fee, MAX_CONVENIENCE_FEE));
    }

    /** Generates a booking ID — in production this is a Snowflake ID or UUID. */
    private String generateBookingId() {
        return "BMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
