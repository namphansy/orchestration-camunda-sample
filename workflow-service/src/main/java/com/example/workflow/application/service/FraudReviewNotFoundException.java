package com.example.workflow.application.service;

public class FraudReviewNotFoundException extends RuntimeException {

    public FraudReviewNotFoundException(String businessKey) {
        super("Fraud review not found: " + businessKey);
    }
}
