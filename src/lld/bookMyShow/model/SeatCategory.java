package lld.bookMyShow.model;

/**
 * Seat category determines pricing tier within a screen.
 * SOLID - SRP: enum purely defines category type.
 */
public enum SeatCategory {
    RECLINER,   // Premium - highest price
    GOLD,       // Mid-tier
    SILVER      // Economy - lowest price
}
