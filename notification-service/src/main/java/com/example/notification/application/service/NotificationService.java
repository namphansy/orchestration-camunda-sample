package com.example.notification.application.service;

import com.example.notification.api.request.PublishNotificationRequest;
import com.example.notification.api.response.NotificationResponse;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationService {

    private final NotificationProperties notificationProperties;

    public NotificationService(NotificationProperties notificationProperties) {
        this.notificationProperties = notificationProperties;
    }

    public NotificationResponse publish(PublishNotificationRequest request) {
        if (notificationProperties.getFailOrderIds().contains(request.orderId())) {
            throw new IllegalStateException("Simulated notification publishing failure");
        }
        return new NotificationResponse(
                UUID.randomUUID().toString(),
                request.orderId(),
                "PUBLISHED",
                normalizeCorrelationId(request.correlationId())
        );
    }

    private String normalizeCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return correlationId;
    }
}
