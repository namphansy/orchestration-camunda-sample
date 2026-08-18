package com.example.workflow.api.response;

public record ReturnWorkflowResponse(
        String processInstanceId,
        String returnId,
        String orderId,
        String correlationId,
        String returnStatus,
        boolean active,
        String failureReason
) {
}