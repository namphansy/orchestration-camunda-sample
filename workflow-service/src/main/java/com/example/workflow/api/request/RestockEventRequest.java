package com.example.workflow.api.request;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public record RestockEventRequest(
        @NotBlank String sku,
        @NotNull @Min(1) Integer quantity,
        String correlationId
) {
}
