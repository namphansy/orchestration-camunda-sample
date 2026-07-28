package com.example.invoice.application.service;

import com.example.invoice.api.request.GenerateInvoiceRequest;
import com.example.invoice.api.response.InvoiceResponse;
import com.example.invoice.domain.model.Invoice;
import com.example.invoice.domain.repository.InvoiceRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional
    public InvoiceResponse generate(GenerateInvoiceRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        LOGGER.info("Invoice generation requested. orderId={}, correlationId={}, amount={}, currency={}, idempotencyKey={}",
                request.orderId(), request.correlationId(), request.amount(), request.currency(), idempotencyKey);
        InvoiceResponse existingResponse = invoiceRepository.findByIdempotencyKey(idempotencyKey)
                .map(InvoiceResponse::from)
                .orElse(null);
        if (existingResponse != null) {
            LOGGER.info("Invoice generation idempotency hit. orderId={}, correlationId={}, invoiceId={}, status={}",
                    request.orderId(), request.correlationId(), existingResponse.invoiceId(), existingResponse.status());
            return existingResponse;
        }

        InvoiceResponse response = InvoiceResponse.from(invoiceRepository.save(new Invoice(
                UUID.randomUUID().toString(),
                request.orderId(),
                request.amount(),
                request.currency(),
                idempotencyKey,
                normalizeCorrelationId(request.correlationId())
        )));
        LOGGER.info("Invoice generated. orderId={}, correlationId={}, invoiceId={}, status={}",
                request.orderId(), request.correlationId(), response.invoiceId(), response.status());
        return response;
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
