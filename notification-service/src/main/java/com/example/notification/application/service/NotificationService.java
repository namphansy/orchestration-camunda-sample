package com.example.notification.application.service;

import com.example.notification.api.request.PublishNotificationRequest;
import com.example.notification.api.response.NotificationResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationProperties notificationProperties;

    public NotificationService(NotificationProperties notificationProperties) {
        this.notificationProperties = notificationProperties;
    }

    public NotificationResponse publish(PublishNotificationRequest request) {
        LOGGER.info("Notification publish requested. orderId={}, correlationId={}, customerId={}",
                request.orderId(), request.correlationId(), request.customerId());
        if (notificationProperties.getFailOrderIds().contains(request.orderId())) {
            LOGGER.warn("Simulating notification publishing failure. orderId={}, correlationId={}",
                    request.orderId(), request.correlationId());
            throw new IllegalStateException("Simulated notification publishing failure");
        }
        NotificationResponse response = new NotificationResponse(
                UUID.randomUUID().toString(),
                request.orderId(),
                "PUBLISHED",
                normalizeCorrelationId(request.correlationId())
        );
        LOGGER.info("Notification published. orderId={}, correlationId={}, notificationId={}, status={}",
                request.orderId(), response.correlationId(), response.notificationId(), response.status());
        return response;
    }

    private String normalizeCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return correlationId;
    }
}
