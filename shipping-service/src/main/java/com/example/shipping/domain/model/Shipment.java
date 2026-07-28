package com.example.shipping.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @Column(name = "shipment_id", nullable = false, length = 64)
    private String shipmentId;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ShipmentStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected Shipment() {
    }

    public Shipment(String shipmentId, String orderId, String sku, Integer quantity, ShipmentStatus status,
                    String idempotencyKey, String correlationId, String failureReason) {
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.sku = sku;
        this.quantity = quantity;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.correlationId = correlationId;
        this.failureReason = failureReason;
        this.createdAt = Instant.now();
    }

    public void cancel() {
        if (status == ShipmentStatus.CANCELLED) {
            return;
        }
        status = ShipmentStatus.CANCELLED;
        cancelledAt = Instant.now();
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSku() {
        return sku;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
