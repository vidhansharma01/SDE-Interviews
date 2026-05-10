package lld.bookMyShow.strategy;

import lld.bookMyShow.exception.PaymentFailedException;
import lld.bookMyShow.model.Payment;
import lld.bookMyShow.model.PaymentStatus;

import java.util.UUID;

/**
 * UPI payment strategy — simulates UPI gateway integration.
 *
 * In production: calls Razorpay/PhonePe/Paytm API, waits for webhook.
 * Here: simulates success/failure based on amount threshold for demo.
 */
public class UpiPaymentStrategy implements PaymentStrategy {

    private final String upiId; // e.g., "user@paytm"

    public UpiPaymentStrategy(String upiId) {
        if (upiId == null || upiId.isBlank())
            throw new IllegalArgumentException("UPI ID cannot be blank");
        this.upiId = upiId;
    }

    @Override
    public Payment processPayment(String bookingId, double amount, String userId) {
        System.out.printf("[UPI] Initiating payment of ₹%.0f via UPI ID: %s%n", amount, upiId);

        // Simulate gateway call — in production this is async with webhook
        boolean success = simulateGatewayCall(amount);

        if (!success) {
            throw new PaymentFailedException("UPI transaction declined for ID: " + upiId);
        }

        String gatewayRef = "UPI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        System.out.printf("[UPI] Payment successful. Gateway ref: %s%n", gatewayRef);

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
    public String getMethodName() { return "UPI"; }

    /** Simulate network call to UPI gateway. Fails for amounts > ₹5000 in demo. */
    private boolean simulateGatewayCall(double amount) {
        return amount <= 5000;
    }
}
