package com.example.workflow.application.service;

public class ApprovalTaskNotFoundException extends RuntimeException {

    public ApprovalTaskNotFoundException(String taskId) {
        super("Approval task not found: " + taskId);
    }
}
