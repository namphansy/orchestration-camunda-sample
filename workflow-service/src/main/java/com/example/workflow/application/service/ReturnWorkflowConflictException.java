package com.example.workflow.application.service;

public class ReturnWorkflowConflictException extends RuntimeException {

    public ReturnWorkflowConflictException(String message) {
        super(message);
    }
}
