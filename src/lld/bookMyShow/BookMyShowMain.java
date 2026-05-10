package lld.bookMyShow;

import lld.bookMyShow.exception.SeatNotAvailableException;
import lld.bookMyShow.factory.BookingFactory;
import lld.bookMyShow.model.*;
import lld.bookMyShow.observer.*;
import lld.bookMyShow.repository.impl.*;
import lld.bookMyShow.service.BookingService;
import lld.bookMyShow.service.impl.BookingServiceImpl;
import lld.bookMyShow.strategy.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * ═══════════════════════════════════════════════════════════════════════
 * BookMyShow — LLD Demo (Staff Engineer Level)
 * ═══════════════════════════════════════════════════════════════════════
 *
 * This class demonstrates all design patterns working together:
 *
 *  ┌─────────────────────────────────────────────────────────────────┐
 *  │  Pattern         │ Where Used                                    │
 *  ├─────────────────────────────────────────────────────────────────┤
 *  │  Builder         │ Movie, Screen, Show, Booking construction     │
 *  │  Factory         │ BookingFactory → price calc + ID generation   │
 *  │  Strategy        │ UpiPaymentStrategy, CardPaymentStrategy,      │
 *  │                  │   WalletPaymentStrategy — swapped at runtime  │
 *  │  Observer        │ BookingEventPublisher → SMS + Email listeners │
 *  │  State           │ Booking transitions: LOCKED→CONFIRMED→CANCEL  │
 *  │  Repository      │ InMemoryShow/Booking/UserRepository (DIP)     │
 *  │  Singleton       │ BookingFactory, EventPublisher (shared)       │
 *  └─────────────────────────────────────────────────────────────────┘
 *
 *  Scenarios covered:
 *   1. Normal booking flow (UPI payment)
 *   2. Booking with Card payment
 *   3. Wallet top-up + Wallet payment
 *   4. Payment failure (insufficient wallet balance)
 *   5. Booking cancellation + seat release
 *   6. Concurrent booking race — only ONE user wins the same seat
 *   7. Booking history retrieval
 */
public class BookMyShowMain {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("═".repeat(70));
        System.out.println("  BookMyShow LLD — Staff Engineer Demo");
        System.out.println("═".repeat(70));

        // ── 1. Bootstrap the system ──────────────────────────────────────
        BookingService bookingService = bootstrap();

        // ── 2. Run all scenarios ──────────────────────────────────────────
        demoNormalUpiBooking(bookingService);
        demoCardPaymentBooking(bookingService);
        demoWalletPayment(bookingService);
        demoWalletInsufficientBalance(bookingService);
        demoCancellation(bookingService);
        demoConcurrentBooking(bookingService);
        demoBookingHistory(bookingService);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SYSTEM BOOTSTRAP
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Wires all dependencies together (replaces a DI framework like Spring).
     * In production: Spring @Bean methods do this wiring.
     */
    private static BookingService bootstrap() {
        System.out.println("\n[Boot] Initializing BookMyShow system...");

        // ── Repositories (in-memory for demo) ────────────────────────────
        InMemoryShowRepository    showRepo    = new InMemoryShowRepository();
        InMemoryBookingRepository bookingRepo = new InMemoryBookingRepository();
        InMemoryUserRepository    userRepo    = new InMemoryUserRepository();

        // ── Observer: register all notification listeners ─────────────────
        BookingEventPublisher publisher = new BookingEventPublisher();
        publisher.register(new SmsNotificationListener());
        publisher.register(new EmailNotificationListener());

        // ── Factory ───────────────────────────────────────────────────────
        BookingFactory factory = new BookingFactory();

        // ── Service (constructor injection — DIP) ─────────────────────────
        BookingService bookingService = new BookingServiceImpl(
                showRepo, bookingRepo, userRepo, factory, publisher);

        // ── Seed Users ────────────────────────────────────────────────────
        userRepo.save(new User("U1", "Vidhan Sharma",  "vidhan@gmail.com",  "9876543210"));
        userRepo.save(new User("U2", "Priya Kapoor",   "priya@gmail.com",   "9123456789"));
        userRepo.save(new User("U3", "Rahul Mehta",    "rahul@gmail.com",   "9012345678"));

        // ── Seed: Movie ───────────────────────────────────────────────────
        Movie avengers = Movie.builder()
                .movieId("M1")
                .title("Avengers: Endgame")
                .genre("Action")
                .language("English")
                .durationMinutes(182)
                .rating("U/A")
                .cast(List.of("Robert Downey Jr.", "Chris Evans"))
                .build();

        Movie animal = Movie.builder()
                .movieId("M2")
                .title("Animal")
                .genre("Thriller")
                .language("Hindi")
                .durationMinutes(201)
                .rating("A")
                .build();

        // ── Seed: Seats for Screen 1 (5 seats for demo clarity) ───────────
        List<Seat> s1Seats = List.of(
                new Seat("A1", "SCR1", "A", 1, SeatCategory.RECLINER, 350),
                new Seat("A2", "SCR1", "A", 2, SeatCategory.RECLINER, 350),
                new Seat("B1", "SCR1", "B", 1, SeatCategory.GOLD,     250),
                new Seat("B2", "SCR1", "B", 2, SeatCategory.GOLD,     250),
                new Seat("C1", "SCR1", "C", 1, SeatCategory.SILVER,   180)
        );

        Screen screen1 = Screen.builder()
                .screenId("SCR1")
                .theatreId("T1")
                .screenName("Audi 1 - IMAX")
                .seats(s1Seats)
                .build();

        // ── Seed: Seats for Screen 2 (for concurrent test) ────────────────
        List<Seat> s2Seats = List.of(
                new Seat("A1", "SCR2", "A", 1, SeatCategory.RECLINER, 400),
                new Seat("A2", "SCR2", "A", 2, SeatCategory.RECLINER, 400),
                new Seat("B1", "SCR2", "B", 1, SeatCategory.GOLD,     300)
        );

        Screen screen2 = Screen.builder()
                .screenId("SCR2")
                .theatreId("T1")
                .screenName("Audi 2 - 3D")
                .seats(s2Seats)
                .build();

        // ── Seed: Shows ───────────────────────────────────────────────────
        Show show1 = Show.builder()
                .showId("SH1")
                .movie(avengers)
                .screen(screen1)
                .startTime(LocalDateTime.now().plusHours(2))
                .endTime(LocalDateTime.now().plusHours(5))
                .format("IMAX")
                .language("English")
                .priceMap(Map.of(
                        SeatCategory.RECLINER, 350.0,
                        SeatCategory.GOLD,     250.0,
                        SeatCategory.SILVER,   180.0))
                .build();

        Show show2 = Show.builder()
                .showId("SH2")
                .movie(avengers)
                .screen(screen2)
                .startTime(LocalDateTime.now().plusHours(3))
                .endTime(LocalDateTime.now().plusHours(6))
                .format("3D")
                .language("English")
                .priceMap(Map.of(
                        SeatCategory.RECLINER, 400.0,
                        SeatCategory.GOLD,     300.0))
                .build();

        Show show3 = Show.builder()
                .showId("SH3")
                .movie(animal)
                .screen(screen1)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(3))
                .format("2D")
                .language("Hindi")
                .priceMap(Map.of(
                        SeatCategory.RECLINER, 300.0,
                        SeatCategory.GOLD,     200.0,
                        SeatCategory.SILVER,   150.0))
                .build();

        showRepo.save(show1);
        showRepo.save(show2);
        showRepo.save(show3);

        System.out.println("[Boot] System ready. Users: 3 | Shows: 3\n");
        return bookingService;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SCENARIO 1: Normal flow — Lock seats → Pay via UPI → Confirmed
    // ═══════════════════════════════════════════════════════════════════

    private static void demoNormalUpiBooking(BookingService bookingService) {
        printScenario(1, "Normal UPI Booking Flow");

        // Strategy Pattern: inject UPI strategy at runtime
        PaymentStrategy upi = new UpiPaymentStrategy("vidhan@paytm");

        Booking booking = bookingService.lockSeats("U1", "SH1", List.of("A1", "B1"));
        printBooking("After lockSeats()", booking);

        Booking confirmed = bookingService.confirmBooking(booking.getBookingId(), "U1", upi);
        printBooking("After confirmBooking()", confirmed);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SCENARIO 2: Card Payment
    // ═══════════════════════════════════════════════════════════════════

    private static void demoCardPaymentBooking(BookingService bookingService) {
        printScenario(2, "Card Payment for Animal show");

        // Strategy Pattern: swap to Card strategy — BookingService unchanged
        PaymentStrategy card = new CardPaymentStrategy("****4242", "Vidhan Sharma");

        Booking booking   = bookingService.lockSeats("U2", "SH3", List.of("A1", "A2"));
        printBooking("After lockSeats()", booking);

        Booking confirmed = bookingService.confirmBooking(booking.getBookingId(), "U2", card);
        printBooking("After confirmBooking()", confirmed);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SCENARIO 3: Wallet top-up → pay via Wallet
    // ═══════════════════════════════════════════════════════════════════

    private static void demoWalletPayment(BookingService bookingService) {
        printScenario(3, "Wallet Top-up + Wallet Payment");

        WalletPaymentStrategy wallet = new WalletPaymentStrategy();
        wallet.topUp("U3", 1000.0);  // Top-up ₹1000 into U3's wallet

        Booking booking = bookingService.lockSeats("U3", "SH2", List.of("B1"));
        printBooking("After lockSeats()", booking);

        Booking confirmed = bookingService.confirmBooking(booking.getBookingId(), "U3", wallet);
        printBooking("After confirmBooking()", confirmed);

        System.out.printf("  Remaining wallet balance for U3: ₹%.0f%n",
                wallet.getBalance("U3"));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SCENARIO 4: Wallet payment fails — insufficient balance
    // ═══════════════════════════════════════════════════════════════════

    private static void demoWalletInsufficientBalance(BookingService bookingService) {
        printScenario(4, "Wallet Payment Failure — Insufficient Balance");

        WalletPaymentStrategy wallet = new WalletPaymentStrategy();
        wallet.topUp("U1", 50.0);  // Only ₹50 — not enough for ₹400 seat

        try {
            Booking booking = bookingService.lockSeats("U1", "SH2", List.of("A1"));
            printBooking("After lockSeats()", booking);
            // Payment will fail → PaymentFailedException thrown
            bookingService.confirmBooking(booking.getBookingId(), "U1", wallet);
        } catch (Exception e) {
            System.out.println("  ✅ Expected failure caught: " + e.getMessage());
            System.out.println("  Seat A1 remains LOCKED (user can retry with another payment method)");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SCENARIO 5: Booking Cancellation
    // ═══════════════════════════════════════════════════════════════════

    private static void demoCancellation(BookingService bookingService) {
        printScenario(5, "User Cancels a Locked Booking");

        PaymentStrategy upi = new UpiPaymentStrategy("priya@gpay");
        Booking booking = bookingService.lockSeats("U2", "SH2", List.of("A2"));
        printBooking("After lockSeats()", booking);

        Booking cancelled = bookingService.cancelBooking(
                booking.getBookingId(), "U2", "Changed my plans");
        printBooking("After cancelBooking()", cancelled);

        System.out.println("  Seat A2 is now AVAILABLE again for other users.");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SCENARIO 6: Concurrent booking — race condition test
    //  Two threads try to book the SAME seat simultaneously.
    //  Only ONE should succeed — the other must get SeatNotAvailableException.
    // ═══════════════════════════════════════════════════════════════════

    private static void demoConcurrentBooking(BookingService bookingService)
            throws InterruptedException {
        printScenario(6, "Concurrent Booking Race — 2 users, same seat");

        CountDownLatch startLatch  = new CountDownLatch(1); // both threads start together
        CountDownLatch doneLatch   = new CountDownLatch(2); // wait for both to finish
        List<String> results = Collections.synchronizedList(new ArrayList<>());

        // Thread 1: User U1 tries to book seat C1 in SH1
        Thread t1 = new Thread(() -> {
            try {
                startLatch.await(); // wait for signal
                Booking b = bookingService.lockSeats("U1", "SH1", List.of("C1"));
                results.add("✅ U1 WON seat C1 → Booking: " + b.getBookingId());
            } catch (SeatNotAvailableException e) {
                results.add("❌ U1 LOST — seat C1 taken: " + e.getMessage());
            } catch (Exception e) {
                results.add("❌ U1 ERROR: " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        }, "Thread-U1");

        // Thread 2: User U2 tries to book the SAME seat C1 in SH1
        Thread t2 = new Thread(() -> {
            try {
                startLatch.await();
                Booking b = bookingService.lockSeats("U2", "SH1", List.of("C1"));
                results.add("✅ U2 WON seat C1 → Booking: " + b.getBookingId());
            } catch (SeatNotAvailableException e) {
                results.add("❌ U2 LOST — seat C1 taken: " + e.getMessage());
            } catch (Exception e) {
                results.add("❌ U2 ERROR: " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        }, "Thread-U2");

        t1.start();
        t2.start();

        // Release both threads simultaneously — maximum contention
        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);

        results.forEach(r -> System.out.println("  " + r));

        long winners = results.stream().filter(r -> r.startsWith("✅")).count();
        System.out.printf("  Result: %d winner(s) — CORRECT (must be exactly 1)%n%n", winners);
        assert winners == 1 : "Concurrency bug! More than one user booked the same seat.";
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SCENARIO 7: Booking History
    // ═══════════════════════════════════════════════════════════════════

    private static void demoBookingHistory(BookingService bookingService) {
        printScenario(7, "Booking History for User U1");

        List<Booking> history = bookingService.getBookingHistory("U1");
        if (history.isEmpty()) {
            System.out.println("  No bookings found for U1.");
        } else {
            history.forEach(b -> System.out.println("  " + b));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════

    private static void printScenario(int num, String title) {
        System.out.println("\n" + "─".repeat(70));
        System.out.printf("  Scenario %d: %s%n", num, title);
        System.out.println("─".repeat(70));
    }

    private static void printBooking(String label, Booking booking) {
        System.out.printf("  [%-25s] %s%n", label, booking);
    }
}
