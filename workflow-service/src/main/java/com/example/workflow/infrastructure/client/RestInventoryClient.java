package com.example.workflow.infrastructure.client;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(RestInventoryClient.class);

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
            LOGGER.info("Calling inventory-service reservation. orderId={}, correlationId={}, sku={}, quantity={}, idempotencyKey={}",
                    request.orderId(), request.correlationId(), request.sku(), request.quantity(), idempotencyKey);
            ResponseEntity<InventoryReservationResponse> response = restTemplate.exchange(
                    inventoryBaseUrl + "/api/inventory/reservations",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    InventoryReservationResponse.class
            );
            InventoryReservationResponse body = response.getBody();
            if (body != null) {
                LOGGER.info("inventory-service reservation completed. orderId={}, correlationId={}, reservationId={}, status={}",
                        request.orderId(), request.correlationId(), body.reservationId(), body.status());
            }
            return body;
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode() == HttpStatus.CONFLICT) {
                LOGGER.warn("inventory-service reported insufficient stock. orderId={}, correlationId={}, sku={}, quantity={}",
                        request.orderId(), request.correlationId(), request.sku(), request.quantity());
                throw new InsufficientStockException("Inventory service reported insufficient stock");
            }
            throw exception;
        }
    }
}
