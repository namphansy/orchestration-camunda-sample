package com.example.workflow.api.request;

import javax.validation.constraints.NotBlank;

public record StartOrderWorkflowRequest(
        @NotBlank String orderId,
        String correlationId
) {
}
