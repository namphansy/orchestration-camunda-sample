package com.example.payment.api.controller;

import com.example.payment.api.request.ChargePaymentRequest;
import com.example.payment.api.response.PaymentTransactionResponse;
import com.example.payment.application.service.PaymentService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/charges")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentTransactionResponse charge(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ChargePaymentRequest request
    ) {
        return paymentService.charge(request, idempotencyKey);
    }

    @GetMapping("/transactions/{transactionId}")
    public PaymentTransactionResponse getTransaction(@PathVariable String transactionId) {
        return paymentService.getTransaction(transactionId);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, Object> handleTechnicalFailure(IllegalStateException exception) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "service", "payment-service",
                "errorCode", "PAYMENT_TECHNICAL_FAILURE",
                "message", exception.getMessage(),
                "details", Map.of()
        );
    }
}
