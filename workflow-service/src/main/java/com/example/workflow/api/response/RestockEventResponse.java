package com.example.workflow.api.response;

public record RestockEventResponse(
        String businessKey,
        String correlationId,
        String status,
        String reason
) {
}
