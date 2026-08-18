package com.example.order.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CompleteOrderDeliveryRequest(
        @NotNull Instant deliveredAt,
        @NotBlank String correlationId
) {
}