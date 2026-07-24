package com.example.inventory.application.service;

import com.example.inventory.api.request.ReserveInventoryRequest;
import com.example.inventory.api.response.InventoryReservationResponse;
import com.example.inventory.domain.exception.InsufficientStockException;
import com.example.inventory.domain.model.InventoryReservation;
import com.example.inventory.domain.model.StockItem;
import com.example.inventory.domain.repository.InventoryReservationRepository;
import com.example.inventory.domain.repository.StockItemRepository;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryReservationService {

    private final Tracer tracer = GlobalOpenTelemetry.getTracer(InventoryReservationService.class.getName());

    private final InventoryReservationRepository reservationRepository;
    private final StockItemRepository stockItemRepository;

    public InventoryReservationService(
            InventoryReservationRepository reservationRepository,
            StockItemRepository stockItemRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.stockItemRepository = stockItemRepository;
    }

    @Transactional
    public InventoryReservationResponse reserve(ReserveInventoryRequest request, String idempotencyKey) {
        Span span = tracer.spanBuilder("inventory.reserve").startSpan();
        try (Scope ignored = span.makeCurrent()) {
            span.setAttribute("order.id", request.orderId());
            span.setAttribute("correlation.id", normalizeCorrelationId(request.correlationId()));
            span.setAttribute("inventory.sku", request.sku());
            span.setAttribute("inventory.quantity", request.quantity());
            if (idempotencyKey != null) {
                span.setAttribute("idempotency.key", idempotencyKey);
            }

            if (idempotencyKey == null || idempotencyKey.isBlank()) {
                throw new IllegalArgumentException("Idempotency-Key header is required");
            }

            InventoryReservationResponse response = reservationRepository.findByIdempotencyKey(idempotencyKey)
                    .map(InventoryReservationResponse::from)
                    .orElseGet(() -> createReservation(request, idempotencyKey));
            span.setAttribute("inventory.reservation.id", response.reservationId());
            span.setAttribute("inventory.status", response.status().name());
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
    public InventoryReservationResponse getReservation(String reservationId) {
        return reservationRepository.findById(reservationId)
                .map(InventoryReservationResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
    }

    private InventoryReservationResponse createReservation(ReserveInventoryRequest request, String idempotencyKey) {
        StockItem stockItem = stockItemRepository.findById(request.sku())
                .orElseThrow(() -> new InsufficientStockException(request.sku(), request.quantity(), 0));
        if (stockItem.getAvailableQuantity() < request.quantity()) {
            throw new InsufficientStockException(request.sku(), request.quantity(), stockItem.getAvailableQuantity());
        }

        stockItem.reserve(request.quantity());
        InventoryReservation reservation = new InventoryReservation(
                UUID.randomUUID().toString(),
                request.orderId(),
                request.sku(),
                request.quantity(),
                idempotencyKey,
                normalizeCorrelationId(request.correlationId())
        );
        return InventoryReservationResponse.from(reservationRepository.save(reservation));
    }

    private String normalizeCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return correlationId;
    }
}
