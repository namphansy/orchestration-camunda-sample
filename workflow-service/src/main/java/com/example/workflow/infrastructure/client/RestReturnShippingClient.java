package com.example.workflow.infrastructure.client;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestReturnShippingClient implements ReturnShippingClient {

    private final RestTemplate restTemplate;
    private final String shippingBaseUrl;

    public RestReturnShippingClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${shipping.service.base-url}")
            String shippingBaseUrl
    ) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();

        this.shippingBaseUrl = shippingBaseUrl;
    }

    @Override
    public ReturnShipmentResponse createReturnShipment(
            CreateReturnShipmentRequest request,
            String idempotencyKey
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", idempotencyKey);

        ResponseEntity<ReturnShipmentResponse> response =
                restTemplate.exchange(
                        shippingBaseUrl + "/api/return-shipments",
                        HttpMethod.POST,
                        new HttpEntity<>(request, headers),
                        ReturnShipmentResponse.class
                );

        return response.getBody();
    }
}
