package com.example.invoice.api.controller;

import com.example.invoice.api.request.GenerateInvoiceRequest;
import com.example.invoice.api.response.InvoiceResponse;
import com.example.invoice.application.service.InvoiceService;
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
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse generate(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody GenerateInvoiceRequest request
    ) {
        return invoiceService.generate(request, idempotencyKey);
    }

    @GetMapping("/{invoiceId}")
    public InvoiceResponse getInvoice(@PathVariable String invoiceId) {
        return invoiceService.getInvoice(invoiceId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(IllegalArgumentException exception) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "service", "invoice-service",
                "errorCode", "INVOICE_REQUEST_INVALID",
                "message", exception.getMessage(),
                "details", Map.of()
        );
    }
}
