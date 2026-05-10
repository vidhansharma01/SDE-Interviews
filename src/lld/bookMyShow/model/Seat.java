package lld.bookMyShow.model;

/**
 * Represents a seat within a screen.
 *
 * Design: Mutable status (seat state changes frequently).
 * The seatId is the unique identity within a show context.
 *
 * SOLID - SRP: Seat holds only seat data and its own status management.
 *              Does NOT decide if it can be locked (that is SeatLockService's job).
 *
 * Concurrency note: In production, status changes are guarded by
 * SeatLockService using Redis atomic Lua scripts. Here, synchronized
 * methods are used for in-memory correctness demonstration.
 */
public class Seat {

    private final String       seatId;       // e.g. "A1", "B12"
    private final String       screenId;
    private final String       rowLabel;     // "A", "B", "C"
    private final int          seatNumber;   // 1, 2, 3...
    private final SeatCategory category;
    private final double       price;

    private volatile SeatStatus status;      // volatile for visibility across threads
    private String              lockedByUserId;
    private long                lockedUntilMs; // epoch ms when lock expires

    public Seat(String seatId, String screenId, String rowLabel,
                int seatNumber, SeatCategory category, double price) {
        this.seatId     = seatId;
        this.screenId   = screenId;
        this.rowLabel   = rowLabel;
        this.seatNumber = seatNumber;
        this.category   = category;
        this.price      = price;
        this.status     = SeatStatus.AVAILABLE;
    }

    // ── Accessors ────────────────────────────────────────────────────

    public String       getSeatId()          { return seatId; }
    public String       getScreenId()        { return screenId; }
    public String       getRowLabel()        { return rowLabel; }
    public int          getSeatNumber()      { return seatNumber; }
    public SeatCategory getCategory()        { return category; }
    public double       getPrice()           { return price; }
    public SeatStatus   getStatus()          { return status; }
    public String       getLockedByUserId()  { return lockedByUserId; }

    // ── State management ─────────────────────────────────────────────

    public synchronized boolean isAvailable() {
        return status == SeatStatus.AVAILABLE;
    }

    public synchronized boolean isLockedByUser(String userId) {
        return status == SeatStatus.LOCKED
                && userId.equals(lockedByUserId)
                && System.currentTimeMillis() < lockedUntilMs;
    }

    /**
     * Attempt to lock this seat for the given user.
     * Returns true only if the seat was AVAILABLE at the moment of lock.
     * Synchronized to prevent race conditions — only one thread wins.
     */
    public synchronized boolean lock(String userId, long lockDurationMs) {
        if (status != SeatStatus.AVAILABLE) return false;
        this.status          = SeatStatus.LOCKED;
        this.lockedByUserId  = userId;
        this.lockedUntilMs   = System.currentTimeMillis() + lockDurationMs;
        return true;
    }

    /** Release back to AVAILABLE. Called on lock expiry or payment failure. */
    public synchronized void release() {
        this.status         = SeatStatus.AVAILABLE;
        this.lockedByUserId = null;
        this.lockedUntilMs  = 0;
    }

    /**
     * Confirm the seat as booked. Only valid if locked by the same user.
     * Returns false if the lock has expired or belongs to another user.
     */
    public synchronized boolean confirm(String userId) {
        if (!isLockedByUser(userId)) return false;
        this.status = SeatStatus.BOOKED;
        return true;
    }

    public synchronized void block() {
        this.status = SeatStatus.BLOCKED;
    }

    @Override
    public String toString() {
        return String.format("Seat{id='%s', category=%s, price=%.0f, status=%s}",
                seatId, category, price, status);
    }
}
