package com.example.invoice.api.response;

import com.example.invoice.domain.model.Invoice;
import com.example.invoice.domain.model.InvoiceStatus;
import java.math.BigDecimal;

public record InvoiceResponse(
        String invoiceId,
        String orderId,
        BigDecimal amount,
        String currency,
        InvoiceStatus status,
        String correlationId
) {

    public static InvoiceResponse from(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getInvoiceId(),
                invoice.getOrderId(),
                invoice.getAmount(),
                invoice.getCurrency(),
                invoice.getStatus(),
                invoice.getCorrelationId()
        );
    }
}
