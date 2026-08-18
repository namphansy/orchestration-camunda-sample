package com.example.shipping.api.request;

import jakarta.validation.constraints.NotBlank;

public record CreateReturnShipmentRequest(
        @NotBlank String returnId,
        @NotBlank String orderId,
        @NotBlank String customerId,
        @NotBlank String correlationId
) {
}