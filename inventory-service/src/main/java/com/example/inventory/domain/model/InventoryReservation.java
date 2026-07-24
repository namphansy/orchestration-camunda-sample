package com.example.inventory.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation {

    @Id
    @Column(name = "reservation_id", nullable = false, length = 64)
    private String reservationId;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ReservationStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InventoryReservation() {
    }

    public InventoryReservation(String reservationId, String orderId, String sku, Integer quantity,
                                String idempotencyKey, String correlationId) {
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.sku = sku;
        this.quantity = quantity;
        this.idempotencyKey = idempotencyKey;
        this.correlationId = correlationId;
        this.status = ReservationStatus.RESERVED;
        this.createdAt = Instant.now();
    }

    public String getReservationId() {
        return reservationId;
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

    public ReservationStatus getStatus() {
        return status;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
