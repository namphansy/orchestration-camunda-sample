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
public class RestPaymentClient implements PaymentClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestPaymentClient.class);

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
        LOGGER.info("Calling payment-service charge. orderId={}, correlationId={}, amount={}, currency={}, idempotencyKey={}",
                request.orderId(), request.correlationId(), request.amount(), request.currency(), idempotencyKey);
        ResponseEntity<PaymentChargeResponse> response = restTemplate.exchange(
                paymentBaseUrl + "/api/payments/charges",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                PaymentChargeResponse.class
        );
        PaymentChargeResponse chargeResponse = response.getBody();
        if (chargeResponse != null && "DECLINED".equals(chargeResponse.status())) {
            LOGGER.warn("payment-service declined charge. orderId={}, correlationId={}, transactionId={}, reason={}",
                    request.orderId(), request.correlationId(), chargeResponse.transactionId(), chargeResponse.failureReason());
            throw new PaymentDeclinedException(chargeResponse.failureReason());
        }
        if (chargeResponse != null) {
            LOGGER.info("payment-service charge completed. orderId={}, correlationId={}, transactionId={}, status={}",
                    request.orderId(), request.correlationId(), chargeResponse.transactionId(), chargeResponse.status());
        }
        return chargeResponse;
    }

    @Override
    public PaymentChargeResponse refundPayment(String transactionId, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", idempotencyKey);
        LOGGER.info("Calling payment-service refund. transactionId={}, idempotencyKey={}",
                transactionId, idempotencyKey);
        ResponseEntity<PaymentChargeResponse> response = restTemplate.exchange(
                paymentBaseUrl + "/api/payments/transactions/" + transactionId + "/refund",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                PaymentChargeResponse.class
        );
        PaymentChargeResponse body = response.getBody();
        if (body != null) {
            LOGGER.info("payment-service refund completed. transactionId={}, orderId={}, status={}",
                    transactionId, body.orderId(), body.status());
        }
        return body;
    }
}
