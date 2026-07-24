package com.example.payment.application.service;

import com.example.payment.api.request.ChargePaymentRequest;
import com.example.payment.api.response.PaymentTransactionResponse;
import com.example.payment.domain.model.PaymentSimulation;
import com.example.payment.domain.model.PaymentStatus;
import com.example.payment.domain.model.PaymentTransaction;
import com.example.payment.domain.repository.PaymentTransactionRepository;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentService {

    private final Tracer tracer = GlobalOpenTelemetry.getTracer(PaymentService.class.getName());

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentProperties paymentProperties;

    public PaymentService(PaymentTransactionRepository transactionRepository, PaymentProperties paymentProperties) {
        this.transactionRepository = transactionRepository;
        this.paymentProperties = paymentProperties;
    }

    @Transactional
    public PaymentTransactionResponse charge(ChargePaymentRequest request, String idempotencyKey) {
        Span span = tracer.spanBuilder("payment.charge").startSpan();
        try (Scope ignored = span.makeCurrent()) {
            span.setAttribute("order.id", request.orderId());
            span.setAttribute("payment.amount", request.amount().doubleValue());
            span.setAttribute("payment.currency", request.currency());
            span.setAttribute("correlation.id", normalizeCorrelationId(request.correlationId()));
            if (idempotencyKey != null) {
                span.setAttribute("idempotency.key", idempotencyKey);
            }

            if (idempotencyKey == null || idempotencyKey.isBlank()) {
                throw new IllegalArgumentException("Idempotency-Key header is required");
            }

            PaymentTransactionResponse existingResponse = transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .map(PaymentTransactionResponse::from)
                    .orElse(null);
            if (existingResponse != null) {
                span.setAttribute("payment.transaction.id", existingResponse.transactionId());
                span.setAttribute("payment.status", existingResponse.status().name());
                return existingResponse;
            }

            simulateDelay();
            simulateTechnicalFailure(request);

            PaymentTransactionResponse response = createTransaction(request, idempotencyKey);
            span.setAttribute("payment.transaction.id", response.transactionId());
            span.setAttribute("payment.status", response.status().name());
            return response;
        } catch (RuntimeException exception) {
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR, exception.getMessage());
            throw exception;
        } finally {
            span.end();
        }
    }

    @Transactional(readOnly = true)
    public PaymentTransactionResponse getTransaction(String transactionId) {
        return transactionRepository.findById(transactionId)
                .map(PaymentTransactionResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Payment transaction not found: " + transactionId));
    }

    private PaymentTransactionResponse createTransaction(ChargePaymentRequest request, String idempotencyKey) {
        PaymentStatus status = shouldDecline(request) ? PaymentStatus.DECLINED : PaymentStatus.CHARGED;
        String failureReason = status == PaymentStatus.DECLINED ? "Payment declined by configured rule" : null;
        PaymentTransaction transaction = new PaymentTransaction(
                UUID.randomUUID().toString(),
                request.orderId(),
                request.amount(),
                request.currency(),
                status,
                idempotencyKey,
                normalizeCorrelationId(request.correlationId()),
                failureReason
        );
        return PaymentTransactionResponse.from(transactionRepository.save(transaction));
    }

    private boolean shouldDecline(ChargePaymentRequest request) {
        return request.simulation() == PaymentSimulation.DECLINE
                || request.amount().compareTo(paymentProperties.declineAbove()) > 0;
    }

    private void simulateTechnicalFailure(ChargePaymentRequest request) {
        if (request.simulation() == PaymentSimulation.TECHNICAL_FAILURE
                || paymentProperties.technicalFailureOrderIds().contains(request.orderId())) {
            throw new IllegalStateException("Simulated payment processor outage");
        }
    }

    private void simulateDelay() {
        if (paymentProperties.processingDelay().isZero() || paymentProperties.processingDelay().isNegative()) {
            return;
        }
        try {
            Thread.sleep(paymentProperties.processingDelay().toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during configured payment delay", exception);
        }
    }

    private String normalizeCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return correlationId;
    }
}
