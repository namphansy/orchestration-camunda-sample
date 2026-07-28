package com.example.workflow.infrastructure.client;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestShippingClient implements ShippingClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestShippingClient.class);

    private final RestTemplate restTemplate;
    private final String shippingBaseUrl;

    public RestShippingClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${shipping.service.base-url}") String shippingBaseUrl
    ) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.shippingBaseUrl = shippingBaseUrl;
    }

    @Override
    public ShipmentResponse createShipment(CreateShipmentRequest request, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", idempotencyKey);
        LOGGER.info("Calling shipping-service shipment creation. orderId={}, correlationId={}, idempotencyKey={}",
                request.orderId(), request.correlationId(), idempotencyKey);
        ResponseEntity<ShipmentResponse> response = restTemplate.exchange(
                shippingBaseUrl + "/api/shipments",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                ShipmentResponse.class
        );
        ShipmentResponse body = response.getBody();
        if (body != null && "FAILED".equals(body.status())) {
            LOGGER.warn("shipping-service failed shipment. orderId={}, correlationId={}, shipmentId={}, reason={}",
                    request.orderId(), request.correlationId(), body.shipmentId(), body.failureReason());
            throw new ShippingFailedException(body.failureReason());
        }
        if (body != null) {
            LOGGER.info("shipping-service shipment completed. orderId={}, correlationId={}, shipmentId={}, status={}",
                    request.orderId(), request.correlationId(), body.shipmentId(), body.status());
        }
        return body;
    }
}
