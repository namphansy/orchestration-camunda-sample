package com.example.invoice.api.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record GenerateInvoiceRequest(
        @NotBlank String orderId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String currency,
        String correlationId
) {
}
