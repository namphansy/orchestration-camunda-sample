package com.example.workflow.infrastructure.client;

public interface ShippingClient {

    ShipmentResponse createShipment(CreateShipmentRequest request, String idempotencyKey);

    record CreateShipmentRequest(
            String orderId,
            String sku,
            Integer quantity,
            String correlationId
    ) {
    }

    record ShipmentResponse(
            String shipmentId,
            String orderId,
            String sku,
            Integer quantity,
            String status,
            String correlationId,
            String failureReason
    ) {
    }
}
