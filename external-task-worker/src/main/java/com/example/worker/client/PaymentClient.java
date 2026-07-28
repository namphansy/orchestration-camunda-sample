package com.example.worker.client;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PaymentClient {

    private final RestTemplate restTemplate;
    private final String paymentBaseUrl;

    public PaymentClient(RestTemplate restTemplate, @Value("${payment.service.base-url}") String paymentBaseUrl) {
        this.restTemplate = restTemplate;
        this.paymentBaseUrl = paymentBaseUrl;
    }

    public PaymentChargeResponse chargePayment(PaymentChargeRequest request, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", idempotencyKey);
        ResponseEntity<PaymentChargeResponse> response = restTemplate.exchange(
                paymentBaseUrl + "/api/payments/charges",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                PaymentChargeResponse.class
        );
        PaymentChargeResponse body = response.getBody();
        if (body != null && "DECLINED".equals(body.status())) {
            throw new PaymentDeclinedException(body.failureReason());
        }
        return body;
    }

    public record PaymentChargeRequest(String orderId, BigDecimal amount, String currency, String correlationId) {
    }

    public record PaymentChargeResponse(
            String transactionId,
            String orderId,
            BigDecimal amount,
            String currency,
            String status,
            String correlationId,
            String failureReason
    ) {
    }
}
