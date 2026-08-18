package com.example.workflow.infrastructure.client;

public interface ReturnShippingClient {

    ReturnShipmentResponse createReturnShipment(
            CreateReturnShipmentRequest request,
            String idempotencyKey
    );

    record CreateReturnShipmentRequest(
            String returnId,
            String orderId,
            String customerId,
            String correlationId
    ) {
    }

    record ReturnShipmentResponse(
            String returnShipmentId,
            String returnId,
            String orderId,
            String customerId,
            String status,
            String correlationId
    ) {
    }
}