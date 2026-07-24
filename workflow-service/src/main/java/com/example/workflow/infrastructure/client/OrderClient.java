package com.example.workflow.infrastructure.client;

import java.math.BigDecimal;

public interface OrderClient {

    OrderDetailsResponse getOrder(String orderId);

    record OrderDetailsResponse(
            String orderId,
            String customerId,
            BigDecimal orderAmount,
            String currency,
            String sku,
            Integer quantity,
            String status,
            String correlationId
    ) {
    }
}
