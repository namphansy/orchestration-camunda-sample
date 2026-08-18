package com.example.workflow.application.service;

public class FraudReviewTaskNotFoundException extends RuntimeException {

    public FraudReviewTaskNotFoundException(String taskId) {
        super("Fraud review task not found: " + taskId);
    }
}
