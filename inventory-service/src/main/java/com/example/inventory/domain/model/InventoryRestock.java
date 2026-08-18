package com.example.inventory.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "inventory_restocks")
public class InventoryRestock {

    @Id
    @Column(name = "restock_id", nullable = false, length = 64)
    private String restockId;

    @Column(name = "return_id", nullable = false, length = 64)
    private String returnId;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private InventoryRestockStatus status;

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

    protected InventoryRestock() {
    }

    public InventoryRestock(
            String restockId,
            String returnId,
            String orderId,
            String sku,
            Integer quantity,
            InventoryRestockStatus status,
            String idempotencyKey,
            String correlationId
    ) {
        this.restockId = restockId;
        this.returnId = returnId;
        this.orderId = orderId;
        this.sku = sku;
        this.quantity = quantity;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.correlationId = correlationId;
        this.createdAt = Instant.now();
    }

    public String getRestockId() {
        return restockId;
    }

    public String getReturnId() {
        return returnId;
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

    public InventoryRestockStatus getStatus() {
        return status;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}