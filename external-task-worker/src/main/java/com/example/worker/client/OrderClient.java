package com.example.worker.client;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class OrderClient {

    private final RestTemplate restTemplate;
    private final String orderBaseUrl;

    public OrderClient(RestTemplate restTemplate, @Value("${order.service.base-url}") String orderBaseUrl) {
        this.restTemplate = restTemplate;
        this.orderBaseUrl = orderBaseUrl;
    }

    public OrderDetailsResponse getOrder(String orderId) {
        return restTemplate.getForObject(orderBaseUrl + "/api/orders/{orderId}", OrderDetailsResponse.class, orderId);
    }

    public record OrderDetailsResponse(
            String orderId,
            String customerId,
            BigDecimal orderAmount,
            String currency,
            String sku,
            Integer quantity,
            String status,
            String correlationId,
            List<OrderLine> orderLines
    ) {
        public List<OrderLine> resolvedOrderLines() {
            if (orderLines != null && !orderLines.isEmpty()) {
                return orderLines;
            }
            return List.of(new OrderLine(sku, quantity));
        }
    }

    public record OrderLine(String sku, Integer quantity) {
    }
}
