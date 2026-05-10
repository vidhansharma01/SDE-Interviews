package lld.bookMyShow.observer;

/**
 * SMS Notification Listener — concrete Observer implementation.
 *
 * SOLID - SRP: Only responsible for sending SMS. No booking logic.
 * SOLID - OCP: Added by registering with publisher — zero changes to BookingService.
 *
 * Only reacts to events it cares about (CONFIRMED, CANCELLED).
 * Ignores LOCKED — no need to SMS user for a lock.
 */
public class SmsNotificationListener implements BookingEventListener {

    @Override
    public void onEvent(BookingEvent event) {
        switch (event.getType()) {
            case BOOKING_CONFIRMED -> sendConfirmationSms(event);
            case BOOKING_CANCELLED -> sendCancellationSms(event);
            case BOOKING_EXPIRED   -> sendExpirySms(event);
            default                -> { /* No SMS for LOCKED */ }
        }
    }

    private void sendConfirmationSms(BookingEvent event) {
        String phone   = event.getUser().getPhone();
        String seats   = event.getBooking().getSeatIds().toString();
        double amount  = event.getBooking().getTotalAmount();
        System.out.printf("[SMS] → %s : Your booking is CONFIRMED! Seats: %s | Amount: ₹%.0f | QR: %s%n",
                phone, seats, amount, event.getBooking().getQrCode());
    }

    private void sendCancellationSms(BookingEvent event) {
        System.out.printf("[SMS] → %s : Booking CANCELLED. Refund of ₹%.0f will be processed in 5-7 days.%n",
                event.getUser().getPhone(), event.getBooking().getTotalAmount());
    }

    private void sendExpirySms(BookingEvent event) {
        System.out.printf("[SMS] → %s : Your seat hold has EXPIRED. Please rebook.%n",
                event.getUser().getPhone());
    }
}
