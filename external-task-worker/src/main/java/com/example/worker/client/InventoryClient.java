package com.example.worker.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class InventoryClient {

    private final RestTemplate restTemplate;
    private final String inventoryBaseUrl;

    public InventoryClient(RestTemplate restTemplate, @Value("${inventory.service.base-url}") String inventoryBaseUrl) {
        this.restTemplate = restTemplate;
        this.inventoryBaseUrl = inventoryBaseUrl;
    }

    public InventoryReservationResponse reserveInventory(InventoryReservationRequest request, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", idempotencyKey);
        try {
            ResponseEntity<InventoryReservationResponse> response = restTemplate.exchange(
                    inventoryBaseUrl + "/api/inventory/reservations",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    InventoryReservationResponse.class
            );
            return response.getBody();
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(409))) {
                throw new InsufficientStockException("Inventory service reported insufficient stock");
            }
            throw exception;
        }
    }

    public record InventoryReservationRequest(String orderId, String sku, Integer quantity, String correlationId) {
    }

    public record InventoryReservationResponse(
            String reservationId,
            String orderId,
            String sku,
            Integer quantity,
            String status,
            String correlationId
    ) {
    }
}
