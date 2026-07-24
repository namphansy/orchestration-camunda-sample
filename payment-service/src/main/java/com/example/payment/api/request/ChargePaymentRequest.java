package com.example.payment.api.request;

import com.example.payment.domain.model.PaymentSimulation;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record ChargePaymentRequest(
        @NotBlank String orderId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        String correlationId,
        PaymentSimulation simulation
) {
}
