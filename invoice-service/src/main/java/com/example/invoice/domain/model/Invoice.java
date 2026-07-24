package com.example.invoice.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @Column(name = "invoice_id", nullable = false, length = 64)
    private String invoiceId;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private InvoiceStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Invoice() {
    }

    public Invoice(String invoiceId, String orderId, BigDecimal amount, String currency,
                   String idempotencyKey, String correlationId) {
        this.invoiceId = invoiceId;
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.correlationId = correlationId;
        this.status = InvoiceStatus.GENERATED;
        this.createdAt = Instant.now();
    }

    public String getInvoiceId() {
        return invoiceId;
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

    public InvoiceStatus getStatus() {
        return status;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
