package com.example.inventory.api.response;

import com.example.inventory.domain.model.InventoryReservation;
import com.example.inventory.domain.model.ReservationStatus;

public record InventoryReservationResponse(
        String reservationId,
        String orderId,
        String sku,
        Integer quantity,
        ReservationStatus status,
        String correlationId
) {
    public static InventoryReservationResponse from(InventoryReservation reservation) {
        return new InventoryReservationResponse(
                reservation.getReservationId(),
                reservation.getOrderId(),
                reservation.getSku(),
                reservation.getQuantity(),
                reservation.getStatus(),
                reservation.getCorrelationId()
        );
    }
}
