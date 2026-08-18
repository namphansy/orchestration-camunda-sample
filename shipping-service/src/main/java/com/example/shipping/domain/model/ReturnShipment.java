package com.example.shipping.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "return_shipments")
public class ReturnShipment {

    @Id
    @Column(
            name = "return_shipment_id",
            nullable = false,
            length = 64
    )
    private String returnShipmentId;

    @Column(
            name = "return_id",
            nullable = false,
            unique = true,
            length = 64
    )
    private String returnId;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ReturnShipmentStatus status;

    @Column(
            name = "idempotency_key",
            nullable = false,
            unique = true,
            length = 128
    )
    private String idempotencyKey;

    @Column(
            name = "correlation_id",
            nullable = false,
            length = 64
    )
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReturnShipment() {
    }

    public ReturnShipment(
            String returnShipmentId,
            String returnId,
            String orderId,
            String customerId,
            ReturnShipmentStatus status,
            String idempotencyKey,
            String correlationId
    ) {
        this.returnShipmentId = returnShipmentId;
        this.returnId = returnId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.correlationId = correlationId;
        this.createdAt = Instant.now();
    }

    public String getReturnShipmentId() {
        return returnShipmentId;
    }

    public String getReturnId() {
        return returnId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public ReturnShipmentStatus getStatus() {
        return status;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}