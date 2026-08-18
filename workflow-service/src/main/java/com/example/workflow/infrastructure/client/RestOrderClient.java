package com.example.workflow.infrastructure.client;

import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestOrderClient implements OrderClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestOrderClient.class);

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
        LOGGER.info("Fetching order details from order-service. orderId={}", orderId);
        OrderDetailsResponse response = restTemplate.getForObject(
                orderBaseUrl + "/api/orders/{orderId}",
                OrderDetailsResponse.class,
                orderId
        );
        if (response != null) {
            LOGGER.info("Fetched order details from order-service. orderId={}, correlationId={}, status={}",
                    orderId, response.correlationId(), response.status());
        }
        return response;
    }

    @Override
    public void updateOrderStatus(String orderId, String status) {
        LOGGER.info("Updating order status in order-service. orderId={}, status={}", orderId, status);
        restTemplate.patchForObject(
                orderBaseUrl + "/api/orders/{orderId}/status",
                Map.of("status", status),
                Void.class,
                orderId
        );
    }
}
