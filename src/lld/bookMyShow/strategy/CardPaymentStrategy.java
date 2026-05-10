package lld.bookMyShow.strategy;

import lld.bookMyShow.exception.PaymentFailedException;
import lld.bookMyShow.model.Payment;
import lld.bookMyShow.model.PaymentStatus;

import java.util.UUID;

/** Credit/Debit Card payment strategy. */
public class CardPaymentStrategy implements PaymentStrategy {

    private final String maskedCardNumber; // last 4 digits: "****1234"
    private final String cardHolderName;

    public CardPaymentStrategy(String maskedCardNumber, String cardHolderName) {
        this.maskedCardNumber = maskedCardNumber;
        this.cardHolderName   = cardHolderName;
    }

    @Override
    public Payment processPayment(String bookingId, double amount, String userId) {
        System.out.printf("[CARD] Charging ₹%.0f to card %s (holder: %s)%n",
                amount, maskedCardNumber, cardHolderName);

        // Simulate 3D Secure + bank authorization
        boolean authorized = simulateBankAuthorization(amount);
        if (!authorized) {
            throw new PaymentFailedException("Card authorization failed for: " + maskedCardNumber);
        }

        String gatewayRef = "CARD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        System.out.printf("[CARD] Authorization successful. Ref: %s%n", gatewayRef);

        return new Payment(
                UUID.randomUUID().toString(),
                bookingId,
                amount,
                getMethodName(),
                PaymentStatus.SUCCESS,
                gatewayRef
        );
    }

    @Override
    public String getMethodName() { return "CARD"; }

    private boolean simulateBankAuthorization(double amount) {
        return amount <= 10000; // Bank limit simulation
    }
}
