package com.example.workflow.api.response;

public record OrderWorkflowResponse(
        String processInstanceId,
        String businessKey,
        String correlationId,
        String status
) {
}

