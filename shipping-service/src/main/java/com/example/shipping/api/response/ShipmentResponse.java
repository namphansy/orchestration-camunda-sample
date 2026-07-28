package com.example.shipping.api.response;

import com.example.shipping.domain.model.Shipment;
import com.example.shipping.domain.model.ShipmentStatus;

public record ShipmentResponse(
        String shipmentId,
        String orderId,
        String sku,
        Integer quantity,
        ShipmentStatus status,
        String correlationId,
        String failureReason
) {
    public static ShipmentResponse from(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getShipmentId(),
                shipment.getOrderId(),
                shipment.getSku(),
                shipment.getQuantity(),
                shipment.getStatus(),
                shipment.getCorrelationId(),
                shipment.getFailureReason()
        );
    }
}
