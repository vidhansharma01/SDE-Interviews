package lld.bookMyShow.exception;

public class BookingNotFoundException extends BookMyShowException {
    public BookingNotFoundException(String bookingId) {
        super("Booking not found: '" + bookingId + "'");
    }
}
