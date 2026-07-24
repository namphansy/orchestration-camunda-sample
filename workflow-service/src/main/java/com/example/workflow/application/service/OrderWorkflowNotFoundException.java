package com.example.workflow.application.service;

public class OrderWorkflowNotFoundException extends RuntimeException {

    public OrderWorkflowNotFoundException(String businessKey) {
        super("Order workflow not found for business key: " + businessKey);
    }
}

