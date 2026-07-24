package com.example.notification.api.request;

import jakarta.validation.constraints.NotBlank;

public record PublishNotificationRequest(
        @NotBlank String orderId,
        @NotBlank String customerId,
        @NotBlank String channel,
        @NotBlank String message,
        String correlationId
) {
}
