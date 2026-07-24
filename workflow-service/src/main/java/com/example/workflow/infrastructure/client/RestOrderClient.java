package com.example.workflow.infrastructure.client;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestOrderClient implements OrderClient {

    private final RestTemplate restTemplate;
    private final String orderBaseUrl;

    public RestOrderClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${order.service.base-url}") String orderBaseUrl
    ) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.orderBaseUrl = orderBaseUrl;
    }

    @Override
    public OrderDetailsResponse getOrder(String orderId) {
        return restTemplate.getForObject(
                orderBaseUrl + "/api/orders/{orderId}",
                OrderDetailsResponse.class,
                orderId
        );
    }
}
