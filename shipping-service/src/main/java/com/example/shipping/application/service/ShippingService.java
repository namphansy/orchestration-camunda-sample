package com.example.shipping.application.service;

import com.example.shipping.api.request.CreateShipmentRequest;
import com.example.shipping.api.response.ShipmentResponse;
import com.example.shipping.domain.model.Shipment;
import com.example.shipping.domain.model.ShipmentSimulation;
import com.example.shipping.domain.model.ShipmentStatus;
import com.example.shipping.domain.repository.ShipmentRepository;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShippingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShippingService.class);

    private final Tracer tracer = GlobalOpenTelemetry.getTracer(ShippingService.class.getName());

    private final ShipmentRepository shipmentRepository;

    public ShippingService(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    @Transactional
    public ShipmentResponse createShipment(CreateShipmentRequest request, String idempotencyKey) {
        Span span = tracer.spanBuilder("shipping.create").startSpan();
        try (Scope ignored = span.makeCurrent()) {
            validateIdempotencyKey(idempotencyKey);
            span.setAttribute("order.id", request.orderId());
            span.setAttribute("correlation.id", normalizeCorrelationId(request.correlationId()));
            span.setAttribute("idempotency.key", idempotencyKey);

            ShipmentResponse existingResponse = shipmentRepository.findByIdempotencyKey(idempotencyKey)
                    .map(ShipmentResponse::from)
                    .orElse(null);
            if (existingResponse != null) {
                LOGGER.info("Shipment idempotency hit. orderId={}, correlationId={}, shipmentId={}, status={}",
                        request.orderId(), request.correlationId(), existingResponse.shipmentId(), existingResponse.status());
                return existingResponse;
            }

            if (request.simulation() == ShipmentSimulation.TECHNICAL_FAILURE) {
                throw new IllegalStateException("Simulated shipping carrier outage");
            }

            ShipmentStatus status = request.simulation() == ShipmentSimulation.CARRIER_FAILURE
                    ? ShipmentStatus.FAILED
                    : ShipmentStatus.CREATED;
            String failureReason = status == ShipmentStatus.FAILED ? "Carrier rejected shipment creation" : null;
            Shipment shipment = new Shipment(
                    UUID.randomUUID().toString(),
                    request.orderId(),
                    request.sku(),
                    request.quantity(),
                    status,
                    idempotencyKey,
                    normalizeCorrelationId(request.correlationId()),
                    failureReason
            );
            ShipmentResponse response = ShipmentResponse.from(shipmentRepository.save(shipment));
            span.setAttribute("shipping.shipment.id", response.shipmentId());
            span.setAttribute("shipping.status", response.status().name());
            LOGGER.info("Shipment created. orderId={}, correlationId={}, shipmentId={}, status={}",
                    request.orderId(), request.correlationId(), response.shipmentId(), response.status());
            return response;
        } catch (RuntimeException exception) {
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR, exception.getMessage());
            throw exception;
        } finally {
            span.end();
        }
    }

    @Transactional
    public ShipmentResponse cancelShipment(String shipmentId, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + shipmentId));
        shipment.cancel();
        return ShipmentResponse.from(shipment);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getShipment(String shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .map(ShipmentResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + shipmentId));
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }
    }

    private String normalizeCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return correlationId;
    }
}
