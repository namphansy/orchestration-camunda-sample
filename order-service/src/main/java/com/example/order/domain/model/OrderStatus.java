package com.example.order.domain.model;

public enum OrderStatus {
    CREATED,
    VALIDATED,
    WAITING_FOR_FRAUD_REVIEW,
    PROCESSING,
    WAITING_FOR_APPROVAL,
    APPROVED,
    REJECTED,
    COMPLETED,
    CANCELLED,
    FAILED
}
