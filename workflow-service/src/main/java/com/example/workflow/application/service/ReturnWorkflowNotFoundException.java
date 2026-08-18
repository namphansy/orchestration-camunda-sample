package com.example.workflow.application.service;

public class ReturnWorkflowNotFoundException extends RuntimeException {

    public ReturnWorkflowNotFoundException(String returnId) {
        super("Return workflow not found: " + returnId);
    }
}
