package com.example.workflow.api.controller;

import com.example.workflow.application.service.OrderWorkflowNotFoundException;
import com.example.workflow.application.service.ApprovalTaskNotFoundException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WorkflowExceptionHandler {

    @ExceptionHandler(OrderWorkflowNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(OrderWorkflowNotFoundException exception) {
        return notFound("WORKFLOW_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ApprovalTaskNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleApprovalTaskNotFound(ApprovalTaskNotFoundException exception) {
        return notFound("APPROVAL_TASK_NOT_FOUND", exception.getMessage());
    }

    private Map<String, Object> notFound(String errorCode, String message) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "service", "workflow-service",
                "errorCode", errorCode,
                "message", message,
                "details", Map.of()
        );
    }
}
