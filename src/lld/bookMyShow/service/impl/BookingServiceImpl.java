package lld.bookMyShow.service.impl;

import lld.bookMyShow.exception.*;
import lld.bookMyShow.factory.BookingFactory;
import lld.bookMyShow.model.*;
import lld.bookMyShow.observer.*;
import lld.bookMyShow.repository.BookingRepository;
import lld.bookMyShow.repository.ShowRepository;
import lld.bookMyShow.repository.UserRepository;
import lld.bookMyShow.service.BookingService;
import lld.bookMyShow.strategy.PaymentStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core implementation of BookingService — the central orchestrator.
 *
 * ── Design Patterns Applied ──────────────────────────────────────────
 * 1. Strategy Pattern : paymentStrategy passed at confirmBooking() call time.
 *    BookingService never knows whether it's UPI, Card or Wallet.
 *
 * 2. Observer Pattern : BookingEventPublisher notifies all registered
 *    listeners (SMS, Email, Push) after each state change.
 *    BookingService never knows SMS/Email logic.
 *
 * 3. Factory Pattern  : BookingFactory.createBooking() handles all
 *    construction + fee calculation. BookingService just calls the factory.
 *
 * 4. Repository Pattern: BookingService depends on BookingRepository
 *    (interface), not on any specific DB implementation.
 *
 * ── SOLID Principles ─────────────────────────────────────────────────
 * S - SRP : This class orchestrates the booking flow only.
 *           Payment logic → Strategy. Notifications → Observer. Fees → Factory.
 * O - OCP : New payment methods → new Strategy. New notifications → new Listener.
 *           BookingServiceImpl never changes for these extensions.
 * L - LSP : Depends on BookingRepository/ShowRepository interfaces.
 *           Swap in-memory for Postgres → no code change here.
 * I - ISP : Implements BookingService only. Payment and notification
 *           are separate concerns with their own interfaces.
 * D - DIP : All dependencies injected via constructor (interfaces, not concretions).
 *
 * ── Concurrency ──────────────────────────────────────────────────────
 * lockSeats() is the critical section. The synchronized block on the Show object
 * ensures only ONE thread can lock seats for a given show at a time.
 * In production: distributed lock (Redis Lua script) replaces this.
 */
public class BookingServiceImpl implements BookingService {

    private static final long LOCK_DURATION_MS = 10 * 60 * 1000L; // 10 minutes

    // Dependencies — all interfaces (DIP)
    private final ShowRepository       showRepository;
    private final BookingRepository    bookingRepository;
    private final UserRepository       userRepository;
    private final BookingFactory       bookingFactory;
    private final BookingEventPublisher eventPublisher;

    /** Constructor injection — all dependencies explicit, no hidden state. */
    public BookingServiceImpl(ShowRepository showRepository,
                              BookingRepository bookingRepository,
                              UserRepository userRepository,
                              BookingFactory bookingFactory,
                              BookingEventPublisher eventPublisher) {
        this.showRepository    = showRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository    = userRepository;
        this.bookingFactory    = bookingFactory;
        this.eventPublisher    = eventPublisher;
    }

    // ── 1. Lock Seats ─────────────────────────────────────────────────

    /**
     * Atomic seat locking — all-or-nothing.
     * Either ALL requested seats are locked, or NONE are (rollback).
     *
     * The synchronized block on `show` provides show-level mutual exclusion.
     * Two users booking DIFFERENT shows proceed in parallel (no unnecessary blocking).
     * Two users booking THE SAME show are serialized — exactly one wins each seat.
     */
    @Override
    public Booking lockSeats(String userId, String showId, List<String> seatIds) {
        // Validate inputs
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BookMyShowException("User not found: " + userId));

        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ShowNotFoundException(showId));

        if (show.hasStarted()) {
            throw new BookMyShowException("Show has already started. Cannot book.");
        }

        System.out.printf("[BookingService] User '%s' attempting to lock seats %s for show '%s'%n",
                userId, seatIds, showId);

        // ── Atomic seat check + lock (synchronized on show instance) ──────────
        List<Seat> lockedSeats;
        synchronized (show) {
            List<String> unavailable = new ArrayList<>();

            // Phase 1: Validate ALL seats are available before locking any
            for (String seatId : seatIds) {
                Seat seat = show.getSeat(seatId);
                if (seat == null || !seat.isAvailable()) {
                    unavailable.add(seatId);
                }
            }

            // All-or-nothing: if any seat unavailable, abort entire lock
            if (!unavailable.isEmpty()) {
                throw new SeatNotAvailableException(unavailable);
            }

            // Phase 2: All seats available — lock them all
            lockedSeats = new ArrayList<>();
            for (String seatId : seatIds) {
                Seat seat = show.getSeat(seatId);
                boolean locked = seat.lock(userId, LOCK_DURATION_MS);
                if (!locked) {
                    // Concurrent theft between Phase 1 and Phase 2 — rollback
                    lockedSeats.forEach(Seat::release);
                    throw new SeatNotAvailableException(List.of(seatId));
                }
                lockedSeats.add(seat);
            }
        }
        // ── End of synchronized block ─────────────────────────────────────────

        // Create and persist the booking record (Factory Pattern)
        Booking booking = bookingFactory.createBooking(userId, show, lockedSeats);
        bookingRepository.save(booking);

        System.out.printf("[BookingService] Seats locked. Booking: %s | Expires in 10 min%n",
                booking.getBookingId());

        // Notify listeners (Observer Pattern)
        eventPublisher.publish(new BookingEvent(BookingEventType.BOOKING_LOCKED, booking, user));

        return booking;
    }

    // ── 2. Confirm Booking ────────────────────────────────────────────

    /**
     * Confirms booking after payment success.
     *
     * Flow:
     *   1. Load booking + validate ownership
     *   2. Process payment via injected Strategy (UPI / Card / Wallet)
     *   3. Confirm seats in Show (LOCKED → BOOKED)
     *   4. Transition booking state (LOCKED → CONFIRMED)
     *   5. Notify observers (SMS, Email)
     *
     * If payment fails → booking stays LOCKED (user can retry within TTL).
     * If seat confirm fails → booking cancelled, payment refund needed (edge case).
     */
    @Override
    public Booking confirmBooking(String bookingId, String userId, PaymentStrategy paymentStrategy) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BookMyShowException("User not found: " + userId));

        // Security: only the booking owner can confirm
        if (!booking.getUserId().equals(userId)) {
            throw new BookMyShowException("Unauthorized: User does not own this booking.");
        }

        if (booking.getStatus() != BookingStatus.LOCKED) {
            throw new BookMyShowException(
                "Cannot confirm booking in status: " + booking.getStatus());
        }

        System.out.printf("[BookingService] Processing payment for booking '%s' via %s%n",
                bookingId, paymentStrategy.getMethodName());

        // ── Strategy Pattern: delegate payment to whichever strategy was injected ─
        Payment payment = paymentStrategy.processPayment(
                bookingId, booking.getTotalAmount(), userId);
        // ─────────────────────────────────────────────────────────────────────────

        // Confirm seats in the show (LOCKED → BOOKED)
        Show show = showRepository.findById(booking.getShowId())
                .orElseThrow(() -> new ShowNotFoundException(booking.getShowId()));

        boolean allConfirmed = true;
        for (String seatId : booking.getSeatIds()) {
            Seat seat = show.getSeat(seatId);
            if (seat == null || !seat.confirm(userId)) {
                allConfirmed = false;
                break;
            }
        }

        if (!allConfirmed) {
            // Seat lock expired between payment and confirm — extremely rare edge case
            // In production: trigger refund async, cancel booking
            booking.cancel("Seat lock expired during payment");
            throw new BookMyShowException(
                "Seat lock expired during payment processing. Refund initiated.");
        }

        // Transition booking to CONFIRMED state
        String qrCode = generateQRCode(booking);
        booking.confirm(payment.getPaymentId(), qrCode);

        System.out.printf("[BookingService] Booking CONFIRMED: %s | QR: %s%n",
                booking.getBookingId(), qrCode);

        // Notify all observers: SMS, Email, Push (Observer Pattern)
        eventPublisher.publish(new BookingEvent(BookingEventType.BOOKING_CONFIRMED, booking, user));

        return booking;
    }

    // ── 3. Cancel Booking ─────────────────────────────────────────────

    @Override
    public Booking cancelBooking(String bookingId, String userId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BookMyShowException("User not found: " + userId));

        if (!booking.getUserId().equals(userId)) {
            throw new BookMyShowException("Unauthorized: User does not own this booking.");
        }

        // Release all locked/booked seats back to AVAILABLE
        Show show = showRepository.findById(booking.getShowId()).orElse(null);
        if (show != null) {
            for (String seatId : booking.getSeatIds()) {
                Seat seat = show.getSeat(seatId);
                if (seat != null) seat.release();
            }
        }

        booking.cancel(reason);
        System.out.printf("[BookingService] Booking CANCELLED: %s | Reason: %s%n",
                bookingId, reason);

        // Notify observers (SMS + Email will handle refund messaging)
        eventPublisher.publish(new BookingEvent(
                BookingEventType.BOOKING_CANCELLED, booking, user, reason));

        return booking;
    }

    // ── 4. Booking History ────────────────────────────────────────────

    @Override
    public List<Booking> getBookingHistory(String userId) {
        return bookingRepository.findByUserId(userId);
    }

    // ── 5. Show browsing ──────────────────────────────────────────────

    @Override
    public List<Show> getShowsForMovie(String movieId) {
        List<Show> shows = showRepository.findByMovieId(movieId);
        System.out.printf("[BookingService] Found %d shows for movie '%s'%n",
                shows.size(), movieId);
        return shows;
    }

    // ── Private helpers ───────────────────────────────────────────────

    /**
     * Generates an encrypted QR code payload.
     * In production: AES-256 encrypt (bookingId + userId + timestamp).
     */
    private String generateQRCode(Booking booking) {
        return "QR-" + booking.getBookingId() + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
