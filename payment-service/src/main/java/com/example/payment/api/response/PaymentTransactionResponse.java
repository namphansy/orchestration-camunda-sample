package com.example.payment.api.response;

import com.example.payment.domain.model.PaymentStatus;
import com.example.payment.domain.model.PaymentTransaction;
import java.math.BigDecimal;

public record PaymentTransactionResponse(
        String transactionId,
        String orderId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String correlationId,
        String failureReason
) {
    public static PaymentTransactionResponse from(PaymentTransaction transaction) {
        return new PaymentTransactionResponse(
                transaction.getTransactionId(),
                transaction.getOrderId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getCorrelationId(),
                transaction.getFailureReason()
        );
    }
}
