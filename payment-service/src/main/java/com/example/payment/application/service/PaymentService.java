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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentService.class);

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

            LOGGER.info("Payment charge requested. orderId={}, correlationId={}, amount={}, currency={}, idempotencyKey={}",
                    request.orderId(), request.correlationId(), request.amount(), request.currency(), idempotencyKey);
            PaymentTransactionResponse existingResponse = transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .map(PaymentTransactionResponse::from)
                    .orElse(null);
            if (existingResponse != null) {
                LOGGER.info("Payment charge idempotency hit. orderId={}, correlationId={}, transactionId={}, status={}",
                        request.orderId(), request.correlationId(), existingResponse.transactionId(), existingResponse.status());
                span.setAttribute("payment.transaction.id", existingResponse.transactionId());
                span.setAttribute("payment.status", existingResponse.status().name());
                return existingResponse;
            }

            simulateDelay();
            simulateTechnicalFailure(request);

            PaymentTransactionResponse response = createTransaction(request, idempotencyKey);
            span.setAttribute("payment.transaction.id", response.transactionId());
            span.setAttribute("payment.status", response.status().name());
            LOGGER.info("Payment transaction created. orderId={}, correlationId={}, transactionId={}, amount={}, currency={}, status={}",
                    request.orderId(), request.correlationId(), response.transactionId(), response.amount(),
                    response.currency(), response.status());
            return response;
        } catch (RuntimeException exception) {
            LOGGER.error("Payment charge failed. orderId={}, correlationId={}, message={}",
                    request.orderId(), request.correlationId(), exception.getMessage(), exception);
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

    @Transactional
    public PaymentTransactionResponse refund(String transactionId, String idempotencyKey) {
        Span span = tracer.spanBuilder("payment.refund").startSpan();
        try (Scope ignored = span.makeCurrent()) {
            if (idempotencyKey == null || idempotencyKey.isBlank()) {
                throw new IllegalArgumentException("Idempotency-Key header is required");
            }
            span.setAttribute("payment.transaction.id", transactionId);
            span.setAttribute("idempotency.key", idempotencyKey);

            PaymentTransaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new IllegalArgumentException("Payment transaction not found: " + transactionId));
            if (transaction.getStatus() == PaymentStatus.DECLINED) {
                LOGGER.info("Skipping refund for declined payment. transactionId={}, orderId={}, idempotencyKey={}",
                        transactionId, transaction.getOrderId(), idempotencyKey);
                return PaymentTransactionResponse.from(transaction);
            }
            transaction.refund(idempotencyKey);
            LOGGER.info("Payment refunded. transactionId={}, orderId={}, correlationId={}, idempotencyKey={}",
                    transactionId, transaction.getOrderId(), transaction.getCorrelationId(), idempotencyKey);
            return PaymentTransactionResponse.from(transaction);
        } catch (RuntimeException exception) {
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR, exception.getMessage());
            throw exception;
        } finally {
            span.end();
        }
    }

    private PaymentTransactionResponse createTransaction(ChargePaymentRequest request, String idempotencyKey) {
        PaymentStatus status = shouldDecline(request) ? PaymentStatus.DECLINED : PaymentStatus.CHARGED;
        String failureReason = status == PaymentStatus.DECLINED ? "Payment declined by configured rule" : null;
        if (status == PaymentStatus.DECLINED) {
            LOGGER.warn("Payment transaction will be declined. orderId={}, correlationId={}, amount={}, currency={}, reason={}",
                    request.orderId(), request.correlationId(), request.amount(), request.currency(), failureReason);
        }
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
        LOGGER.info("shouldDecline check. orderId={}, correlationId={}, amount={}, currency={}, simulation={}",
                request.orderId(), request.correlationId(), request.amount(), request.currency(), request.simulation());
        return request.simulation() == PaymentSimulation.DECLINE
                || request.amount().compareTo(paymentProperties.declineAbove()) > 0;
    }

    private void simulateTechnicalFailure(ChargePaymentRequest request) {
        if (request.simulation() == PaymentSimulation.TECHNICAL_FAILURE
                || paymentProperties.technicalFailureOrderIds().contains(request.orderId())) {
            LOGGER.warn("Simulating payment technical failure. orderId={}, correlationId={}",
                    request.orderId(), request.correlationId());
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
