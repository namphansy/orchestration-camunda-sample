package com.example.shipping.api.response;

import com.example.shipping.domain.model.ReturnShipment;
import com.example.shipping.domain.model.ReturnShipmentStatus;
import java.time.Instant;

public record ReturnShipmentResponse(
        String returnShipmentId,
        String returnId,
        String orderId,
        String customerId,
        ReturnShipmentStatus status,
        String correlationId,
        Instant createdAt
) {
    public static ReturnShipmentResponse from(
            ReturnShipment returnShipment
    ) {
        return new ReturnShipmentResponse(
                returnShipment.getReturnShipmentId(),
                returnShipment.getReturnId(),
                returnShipment.getOrderId(),
                returnShipment.getCustomerId(),
                returnShipment.getStatus(),
                returnShipment.getCorrelationId(),
                returnShipment.getCreatedAt()
        );
    }
}