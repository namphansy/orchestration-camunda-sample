package com.example.order.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "order_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal orderAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;

    @Column(name = "workflow_process_instance_id", length = 64)
    private String workflowProcessInstanceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    protected OrderEntity() {
    }

    public OrderEntity(String orderId, String customerId, BigDecimal orderAmount, String currency,
                       String sku, Integer quantity, String correlationId) {
        Instant now = Instant.now();
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderAmount = orderAmount;
        this.currency = currency;
        this.sku = sku;
        this.quantity = quantity;
        this.correlationId = correlationId;
        this.status = OrderStatus.CREATED;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void markProcessing(String workflowProcessInstanceId) {
        this.workflowProcessInstanceId = workflowProcessInstanceId;
        this.status = OrderStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void markDelivered(Instant deliveredAt) {
        this.status = OrderStatus.COMPLETED;
        this.deliveredAt = deliveredAt;
        this.updatedAt = Instant.now();
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getSku() {
        return sku;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getWorkflowProcessInstanceId() {
        return workflowProcessInstanceId;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }
}
