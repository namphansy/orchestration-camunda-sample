package com.example.workflow.infrastructure.client;

public interface NotificationClient {

    NotificationResponse publishNotification(PublishNotificationRequest request);

    record PublishNotificationRequest(
            String orderId,
            String customerId,
            String channel,
            String message,
            String correlationId
    ) {
    }

    record NotificationResponse(
            String notificationId,
            String orderId,
            String status,
            String correlationId
    ) {
    }
}
