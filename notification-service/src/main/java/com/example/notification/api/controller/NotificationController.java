package com.example.notification.api.controller;

import com.example.notification.api.request.PublishNotificationRequest;
import com.example.notification.api.response.NotificationResponse;
import com.example.notification.application.service.NotificationService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public NotificationResponse publish(@Valid @RequestBody PublishNotificationRequest request) {
        return notificationService.publish(request);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, Object> handlePublishFailure(IllegalStateException exception) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "service", "notification-service",
                "errorCode", "NOTIFICATION_PUBLISH_FAILED",
                "message", exception.getMessage(),
                "details", Map.of()
        );
    }
}
