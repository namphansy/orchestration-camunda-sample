package com.example.workflow.api.request;

import javax.validation.constraints.NotBlank;

public record StartReturnWorkflowRequest(
        @NotBlank String returnId,
        @NotBlank String orderId,
        @NotBlank String customerId,
        @NotBlank String returnReason,
        @NotBlank String correlationId
) {
}