package com.example.invoice.application.service;

import com.example.invoice.api.request.GenerateInvoiceRequest;
import com.example.invoice.api.response.InvoiceResponse;
import com.example.invoice.domain.model.Invoice;
import com.example.invoice.domain.repository.InvoiceRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional
    public InvoiceResponse generate(GenerateInvoiceRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        return invoiceRepository.findByIdempotencyKey(idempotencyKey)
                .map(InvoiceResponse::from)
                .orElseGet(() -> InvoiceResponse.from(invoiceRepository.save(new Invoice(
                        UUID.randomUUID().toString(),
                        request.orderId(),
                        request.amount(),
                        request.currency(),
                        idempotencyKey,
                        normalizeCorrelationId(request.correlationId())
                ))));
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(String invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .map(InvoiceResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
    }

    private String normalizeCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return correlationId;
    }
}
