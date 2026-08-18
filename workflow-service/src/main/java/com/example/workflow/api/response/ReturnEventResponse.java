package com.example.workflow.api.response;

public record ReturnEventResponse(
        String returnId,
        String correlationId,
        String status
) {
}