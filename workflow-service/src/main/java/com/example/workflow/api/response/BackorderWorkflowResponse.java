package com.example.workflow.api.response;

public record BackorderWorkflowResponse(
        String processInstanceId,
        String businessKey,
        String backorderId,
        String orderId,
        String sku,
        Integer quantity,
        String correlationId,
        String backorderStatus,
        String workflowStatus
) {
}
