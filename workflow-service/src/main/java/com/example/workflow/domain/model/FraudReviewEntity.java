package com.example.workflow.domain.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fraud_reviews", indexes = {
        @Index(name = "idx_fr_order_id", columnList = "order_id"),
        @Index(name = "idx_fr_status", columnList = "review_status")
})
public class FraudReviewEntity {

    @Id
    @Column(name = "review_id", nullable = false, length = 64)
    private String reviewId;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "order_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal orderAmount;

    @Column(name = "fraud_score", nullable = false)
    private Integer fraudScore;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "review_status", nullable = false, length = 32)
    private String reviewStatus;

    @Column(name = "assignee", length = 64)
    private String assignee;

    @Column(name = "approver", length = 64)
    private String approver;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "process_instance_id", length = 64)
    private String processInstanceId;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FraudReviewEntity() {
    }

    public FraudReviewEntity(String reviewId, String orderId, String customerId, BigDecimal orderAmount,
                             Integer fraudScore, String correlationId) {
        Instant now = Instant.now();
        this.reviewId = reviewId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderAmount = orderAmount;
        this.fraudScore = fraudScore;
        this.correlationId = correlationId;
        this.reviewStatus = "PENDING";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getReviewId() {
        return reviewId;
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

    public Integer getFraudScore() {
        return fraudScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
        this.updatedAt = Instant.now();
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
        this.updatedAt = Instant.now();
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
        this.updatedAt = Instant.now();
    }

    public String getApprover() {
        return approver;
    }

    public void setApprover(String approver) {
        this.approver = approver;
        this.updatedAt = Instant.now();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
        this.updatedAt = Instant.now();
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
        this.updatedAt = Instant.now();
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
        this.updatedAt = Instant.now();
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
