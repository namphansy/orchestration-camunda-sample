package com.example.workflow.infrastructure.client;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestNotificationClient implements NotificationClient {

    private final RestTemplate restTemplate;
    private final String notificationBaseUrl;

    public RestNotificationClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${notification.service.base-url}") String notificationBaseUrl
    ) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.notificationBaseUrl = notificationBaseUrl;
    }

    @Override
    public NotificationResponse publishNotification(PublishNotificationRequest request) {
        ResponseEntity<NotificationResponse> response = restTemplate.postForEntity(
                notificationBaseUrl + "/api/notifications",
                request,
                NotificationResponse.class
        );
        return response.getBody();
    }
}
