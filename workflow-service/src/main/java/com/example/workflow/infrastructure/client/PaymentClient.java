package com.example.workflow.infrastructure.client;

import java.math.BigDecimal;

public interface PaymentClient {

    PaymentChargeResponse chargePayment(PaymentChargeRequest request, String idempotencyKey);

    PaymentChargeResponse refundPayment(String transactionId, String idempotencyKey);

    record PaymentChargeRequest(
            String orderId,
            BigDecimal amount,
            String currency,
            String correlationId
    ) {
    }

    record PaymentChargeResponse(
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
