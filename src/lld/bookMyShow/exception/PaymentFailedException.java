package lld.bookMyShow.exception;

public class PaymentFailedException extends BookMyShowException {
    public PaymentFailedException(String reason) {
        super("Payment failed: " + reason);
    }
}
