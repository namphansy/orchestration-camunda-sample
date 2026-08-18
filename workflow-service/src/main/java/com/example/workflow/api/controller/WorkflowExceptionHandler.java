package com.example.workflow.api.controller;

import com.example.workflow.application.service.ReturnWorkflowConflictException;
import com.example.workflow.application.service.ReturnWorkflowNotFoundException;
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
        return response("WORKFLOW_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ApprovalTaskNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleApprovalTaskNotFound(ApprovalTaskNotFoundException exception) {
            return response("APPROVAL_TASK_NOT_FOUND", exception.getMessage());
        }

    @ExceptionHandler(ReturnWorkflowNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleReturnNotFound(
            ReturnWorkflowNotFoundException exception
    ) {
        return response(
                "RETURN_WORKFLOW_NOT_FOUND",
                exception.getMessage()
        );
    }

    @ExceptionHandler(ReturnWorkflowConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleReturnConflict(
            ReturnWorkflowConflictException exception
    ) {
        return response(
                "RETURN_WORKFLOW_CONFLICT",
                exception.getMessage()
        );
    }

    private Map<String, Object> response(String errorCode, String message) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "service", "workflow-service",
                "errorCode", errorCode,
                "message", message,
                "details", Map.of()
        );
    }
}
