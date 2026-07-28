package com.example.payment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    @Column(name = "transaction_id", nullable = false, length = 64)
    private String transactionId;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PaymentStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "refund_idempotency_key", length = 128)
    private String refundIdempotencyKey;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    protected PaymentTransaction() {
    }

    public PaymentTransaction(String transactionId, String orderId, BigDecimal amount, String currency,
                              PaymentStatus status, String idempotencyKey, String correlationId,
                              String failureReason) {
        this.transactionId = transactionId;
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.correlationId = correlationId;
        this.failureReason = failureReason;
        this.createdAt = Instant.now();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void refund(String refundIdempotencyKey) {
        if (status == PaymentStatus.REFUNDED) {
            return;
        }
        status = PaymentStatus.REFUNDED;
        this.refundIdempotencyKey = refundIdempotencyKey;
        refundedAt = Instant.now();
    }
}
