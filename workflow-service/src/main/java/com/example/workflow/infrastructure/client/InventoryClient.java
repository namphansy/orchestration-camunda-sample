package com.example.workflow.infrastructure.client;

public interface InventoryClient {

    InventoryReservationResponse reserveInventory(InventoryReservationRequest request, String idempotencyKey);

    record InventoryReservationRequest(
            String orderId,
            String sku,
            Integer quantity,
            String correlationId
    ) {
    }

    record InventoryReservationResponse(
            String reservationId,
            String orderId,
            String sku,
            Integer quantity,
            String status,
            String correlationId
    ) {
    }
}
