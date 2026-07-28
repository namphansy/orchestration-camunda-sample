package com.example.workflow.api.request;

import javax.validation.constraints.NotBlank;

public record ConfirmPaymentRequest(
        @NotBlank String correlationId,
        @NotBlank String paymentTransactionId,
        @NotBlank String paymentStatus,
        String failureReason
) {
}
