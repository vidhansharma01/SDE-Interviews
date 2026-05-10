package lld.bookMyShow.exception;

import java.util.List;

/** Thrown when one or more requested seats are not available for locking. */
public class SeatNotAvailableException extends BookMyShowException {
    private final List<String> unavailableSeatIds;

    public SeatNotAvailableException(List<String> seatIds) {
        super("Seats not available: " + seatIds);
        this.unavailableSeatIds = List.copyOf(seatIds);
    }

    public List<String> getUnavailableSeatIds() { return unavailableSeatIds; }
}
