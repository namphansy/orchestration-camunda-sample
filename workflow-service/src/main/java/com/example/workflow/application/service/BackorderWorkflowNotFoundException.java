package com.example.workflow.application.service;

public class BackorderWorkflowNotFoundException extends RuntimeException {

    public BackorderWorkflowNotFoundException(String businessKey) {
        super("Backorder workflow not found for business key: " + businessKey);
    }
}
