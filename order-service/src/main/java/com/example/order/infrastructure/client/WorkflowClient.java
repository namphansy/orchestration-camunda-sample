package com.example.order.infrastructure.client;

import com.example.order.api.request.CreateOrderRequest;

public interface WorkflowClient {

    WorkflowStartResponse startOrderWorkflow(CreateOrderRequest request, String correlationId);

    record WorkflowStartRequest(
            String orderId,
            String correlationId
    ) {
    }

    record WorkflowStartResponse(
            String processInstanceId,
            String businessKey,
            String correlationId,
            String status
    ) {
    }
}
