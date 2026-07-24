package com.example.order.api.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotBlank String orderId,
        @NotBlank String customerId,
        @NotNull @DecimalMin("0.01") BigDecimal orderAmount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotBlank String sku,
        @NotNull @Min(1) Integer quantity,
        String correlationId
) {
}
