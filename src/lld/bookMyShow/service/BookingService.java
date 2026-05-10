package lld.bookMyShow.service;

import lld.bookMyShow.model.Booking;
import lld.bookMyShow.model.Show;
import lld.bookMyShow.strategy.PaymentStrategy;

import java.util.List;

/**
 * Core booking service interface — defines all booking operations.
 *
 * SOLID - ISP: Interface contains only booking-related methods.
 *              Separate interfaces for show search, payment handling.
 * SOLID - DIP: All callers (controllers, tests) depend on this abstraction.
 */
public interface BookingService {

    /**
     * Lock seats for a user. Returns a Booking in LOCKED state.
     * Seats are held for LOCK_DURATION_MS (10 minutes).
     *
     * @param userId   the user locking seats
     * @param showId   the target show
     * @param seatIds  list of seat IDs to lock
     * @return         Booking in LOCKED status
     * @throws lld.bookMyShow.exception.ShowNotFoundException       if showId invalid
     * @throws lld.bookMyShow.exception.SeatNotAvailableException   if any seat is taken
     */
    Booking lockSeats(String userId, String showId, List<String> seatIds);

    /**
     * Confirm a booking after successful payment.
     * Transitions booking: LOCKED → CONFIRMED.
     *
     * @param bookingId        the booking to confirm
     * @param userId           must match booking owner (security check)
     * @param paymentStrategy  the payment method chosen by the user (Strategy Pattern)
     * @return                 confirmed Booking with QR code
     * @throws lld.bookMyShow.exception.BookingNotFoundException  if bookingId invalid
     * @throws lld.bookMyShow.exception.PaymentFailedException    if payment rejected
     */
    Booking confirmBooking(String bookingId, String userId, PaymentStrategy paymentStrategy);

    /**
     * Cancel a booking. Releases locked seats back to AVAILABLE.
     * Only LOCKED or CONFIRMED bookings can be cancelled.
     *
     * @param bookingId  the booking to cancel
     * @param userId     must match booking owner
     * @param reason     cancellation reason
     * @return           cancelled Booking
     */
    Booking cancelBooking(String bookingId, String userId, String reason);

    /** Returns all bookings for a user (booking history). */
    List<Booking> getBookingHistory(String userId);

    /** Helper: list all shows for a movie — for browsing. */
    List<Show> getShowsForMovie(String movieId);
}
