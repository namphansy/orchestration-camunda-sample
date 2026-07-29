package com.example.worker.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ShippingClient {

    private final RestTemplate restTemplate;
    private final String shippingBaseUrl;

    public ShippingClient(RestTemplate restTemplate, @Value("${shipping.service.base-url}") String shippingBaseUrl) {
        this.restTemplate = restTemplate;
        this.shippingBaseUrl = shippingBaseUrl;
    }

    public ShipmentResponse createShipment(CreateShipmentRequest request, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", idempotencyKey);
        ResponseEntity<ShipmentResponse> response = restTemplate.exchange(
                shippingBaseUrl + "/api/shipments",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                ShipmentResponse.class
        );
        ShipmentResponse body = response.getBody();
        if (body != null && "FAILED".equals(body.status())) {
            throw new ShippingFailedException(body.failureReason());
        }
        return body;
    }

    public record CreateShipmentRequest(String orderId, String sku, Integer quantity, String correlationId) {
    }

    public record ShipmentResponse(
            String shipmentId,
            String orderId,
            String sku,
            Integer quantity,
            String status,
            String correlationId,
            String failureReason
    ) {
    }
}
