package com.example.notification.api.response;

public record NotificationResponse(
        String notificationId,
        String orderId,
        String status,
        String correlationId
) {
}
