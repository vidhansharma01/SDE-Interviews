package lld.bookMyShow.strategy;

import lld.bookMyShow.exception.PaymentFailedException;
import lld.bookMyShow.model.Payment;
import lld.bookMyShow.model.PaymentStatus;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wallet payment strategy — uses BookMyShow internal wallet balance.
 *
 * Demonstrates: strategy with its own state (wallet balance map).
 * In production, wallet balance is stored in a dedicated Wallet Service DB.
 */
public class WalletPaymentStrategy implements PaymentStrategy {

    // userId → wallet balance (in-memory for demo)
    private final ConcurrentHashMap<String, Double> walletBalances;

    public WalletPaymentStrategy() {
        this.walletBalances = new ConcurrentHashMap<>();
    }

    /** Top up wallet balance — for demo setup. */
    public void topUp(String userId, double amount) {
        walletBalances.merge(userId, amount, Double::sum);
        System.out.printf("[WALLET] Topped up ₹%.0f for user: %s. New balance: ₹%.0f%n",
                amount, userId, walletBalances.get(userId));
    }

    @Override
    public Payment processPayment(String bookingId, double amount, String userId) {
        double balance = walletBalances.getOrDefault(userId, 0.0);
        System.out.printf("[WALLET] User '%s' balance: ₹%.0f | Required: ₹%.0f%n",
                userId, balance, amount);

        if (balance < amount) {
            throw new PaymentFailedException(
                String.format("Insufficient wallet balance. Have: ₹%.0f, Need: ₹%.0f", balance, amount));
        }

        // Atomic deduction — in production use DB transaction or CAS
        synchronized (this) {
            double currentBalance = walletBalances.getOrDefault(userId, 0.0);
            if (currentBalance < amount)
                throw new PaymentFailedException("Insufficient balance (concurrent check)");
            walletBalances.put(userId, currentBalance - amount);
        }

        String gatewayRef = "WALLET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        System.out.printf("[WALLET] Deducted ₹%.0f. Remaining: ₹%.0f%n",
                amount, walletBalances.get(userId));

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
    public String getMethodName() { return "WALLET"; }

    public double getBalance(String userId) {
        return walletBalances.getOrDefault(userId, 0.0);
    }
}
