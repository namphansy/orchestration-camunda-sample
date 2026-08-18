package com.example.workflow.api.request;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public record StartFraudReviewRequest(

        @NotBlank(message = "reviewId is required")
        String reviewId,

        @NotBlank(message = "orderId is required")
        String orderId,

        @NotBlank(message = "customerId is required")
        String customerId,

        @NotNull(message = "orderAmount is required")
        @DecimalMin(value = "0.01", message = "orderAmount must be positive")
        BigDecimal orderAmount,

        @NotNull(message = "fraudScore is required")
        @Min(value = 0, message = "fraudScore must be >= 0")
        @Max(value = 100, message = "fraudScore must be <= 100")
        Integer fraudScore,

        String correlationId
) {}
