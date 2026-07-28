package com.example.workflow.api.response;

public record PaymentConfirmationResponse(
        String businessKey,
        String correlationId,
        String status
) {
}
