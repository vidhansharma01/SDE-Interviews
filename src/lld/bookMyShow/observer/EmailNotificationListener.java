package lld.bookMyShow.observer;

/**
 * Email Notification Listener — concrete Observer.
 * Sends rich HTML email with QR code attachment for confirmations.
 */
public class EmailNotificationListener implements BookingEventListener {

    @Override
    public void onEvent(BookingEvent event) {
        switch (event.getType()) {
            case BOOKING_CONFIRMED -> sendTicketEmail(event);
            case BOOKING_CANCELLED -> sendCancellationEmail(event);
            default                -> { /* No email for LOCKED/EXPIRED */ }
        }
    }

    private void sendTicketEmail(BookingEvent event) {
        String email  = event.getUser().getEmail();
        String name   = event.getUser().getName();
        System.out.printf("[EMAIL] → %s : Hi %s! Your e-ticket for booking %s is ready. " +
                "Seats: %s | QR attached.%n",
                email, name, event.getBooking().getBookingId(),
                event.getBooking().getSeatIds());
    }

    private void sendCancellationEmail(BookingEvent event) {
        System.out.printf("[EMAIL] → %s : Booking %s cancelled. Reason: %s%n",
                event.getUser().getEmail(),
                event.getBooking().getBookingId(),
                event.getAdditionalInfo() != null ? event.getAdditionalInfo() : "User requested");
    }
}
