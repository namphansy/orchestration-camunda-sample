package com.example.workflow.infrastructure.client;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class RestInventoryClient implements InventoryClient {

    private final RestTemplate restTemplate;
    private final String inventoryBaseUrl;

    public RestInventoryClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${inventory.service.base-url}") String inventoryBaseUrl
    ) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.inventoryBaseUrl = inventoryBaseUrl;
    }

    @Override
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
            if (exception.getStatusCode() == HttpStatus.CONFLICT) {
                throw new InsufficientStockException("Inventory service reported insufficient stock");
            }
            throw exception;
        }
    }
}
