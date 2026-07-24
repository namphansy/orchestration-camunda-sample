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
public class RestPaymentClient implements PaymentClient {

    private final RestTemplate restTemplate;
    private final String paymentBaseUrl;

    public RestPaymentClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${payment.service.base-url}") String paymentBaseUrl
    ) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.paymentBaseUrl = paymentBaseUrl;
    }

    @Override
    public PaymentChargeResponse chargePayment(PaymentChargeRequest request, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", idempotencyKey);
        ResponseEntity<PaymentChargeResponse> response = restTemplate.exchange(
                paymentBaseUrl + "/api/payments/charges",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                PaymentChargeResponse.class
        );
        PaymentChargeResponse chargeResponse = response.getBody();
        if (chargeResponse != null && "DECLINED".equals(chargeResponse.status())) {
            throw new PaymentDeclinedException(chargeResponse.failureReason());
        }
        return chargeResponse;
    }
}
