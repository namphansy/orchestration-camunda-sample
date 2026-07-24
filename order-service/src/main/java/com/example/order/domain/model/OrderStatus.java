package com.example.order.domain.model;

public enum OrderStatus {
    CREATED,
    VALIDATED,
    PROCESSING,
    WAITING_FOR_APPROVAL,
    APPROVED,
    REJECTED,
    COMPLETED,
    CANCELLED,
    FAILED
}
