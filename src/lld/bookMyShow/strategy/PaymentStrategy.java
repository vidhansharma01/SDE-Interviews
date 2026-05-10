package lld.bookMyShow.strategy;

import lld.bookMyShow.model.Payment;

/**
 * Strategy Pattern — Payment Processing.
 *
 * SOLID - OCP: Adding a new payment method (e.g., CryptoPay) requires only
 *   creating a new class implementing this interface. BookingService never changes.
 *
 * SOLID - LSP: All implementations are interchangeable — BookingService
 *   calls processPayment() without knowing which concrete strategy is used.
 *
 * SOLID - DIP: BookingService depends on PaymentStrategy (abstraction),
 *   not UpiPaymentStrategy or CardPaymentStrategy (concretions).
 *
 * SOLID - ISP: This interface is lean — only the method a payment strategy needs.
 */
public interface PaymentStrategy {

    /**
     * Process a payment for the given booking amount.
     *
     * @param bookingId  the booking being paid for
     * @param amount     the total amount to charge (in INR)
     * @param userId     the payer's user ID
     * @return           Payment record if successful
     * @throws lld.bookMyShow.exception.PaymentFailedException if payment fails
     */
    Payment processPayment(String bookingId, double amount, String userId);

    /**
     * Returns the payment method name — used for logging and Payment record.
     * e.g., "UPI", "CARD", "WALLET"
     */
    String getMethodName();
}
