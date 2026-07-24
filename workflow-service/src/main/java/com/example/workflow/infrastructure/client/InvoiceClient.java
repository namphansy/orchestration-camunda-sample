package com.example.workflow.infrastructure.client;

import java.math.BigDecimal;

public interface InvoiceClient {

    InvoiceResponse generateInvoice(GenerateInvoiceRequest request, String idempotencyKey);

    record GenerateInvoiceRequest(
            String orderId,
            BigDecimal amount,
            String currency,
            String correlationId
    ) {
    }

    record InvoiceResponse(
            String invoiceId,
            String orderId,
            BigDecimal amount,
            String currency,
            String status,
            String correlationId
    ) {
    }
}
