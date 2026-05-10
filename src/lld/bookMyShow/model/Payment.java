package lld.bookMyShow.model;

import java.time.LocalDateTime;

/**
 * Records a payment transaction.
 * Immutable — a payment record is never modified (append-only audit trail).
 * Refunds create a NEW Payment record with status REFUNDED.
 */
public final class Payment {

    private final String        paymentId;
    private final String        bookingId;
    private final double        amount;
    private final String        method;          // "UPI", "CARD", "WALLET"
    private final PaymentStatus status;
    private final LocalDateTime processedAt;
    private final String        gatewayReference; // external gateway txn ID

    public Payment(String paymentId, String bookingId, double amount,
                   String method, PaymentStatus status, String gatewayReference) {
        this.paymentId        = paymentId;
        this.bookingId        = bookingId;
        this.amount           = amount;
        this.method           = method;
        this.status           = status;
        this.gatewayReference = gatewayReference;
        this.processedAt      = LocalDateTime.now();
    }

    public String        getPaymentId()        { return paymentId; }
    public String        getBookingId()        { return bookingId; }
    public double        getAmount()           { return amount; }
    public String        getMethod()           { return method; }
    public PaymentStatus getStatus()           { return status; }
    public LocalDateTime getProcessedAt()      { return processedAt; }
    public String        getGatewayReference() { return gatewayReference; }

    @Override
    public String toString() {
        return String.format("Payment{id='%s', booking='%s', amount=₹%.0f, method='%s', status=%s}",
                paymentId, bookingId, amount, method, status);
    }
}
