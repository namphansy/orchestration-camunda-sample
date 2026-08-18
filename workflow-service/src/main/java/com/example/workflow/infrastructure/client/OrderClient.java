package com.example.workflow.infrastructure.client;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public interface OrderClient {

    OrderDetailsResponse getOrder(String orderId);

    void updateOrderStatus(String orderId, String status);

    record OrderDetailsResponse(
            String orderId,
            String customerId,
            BigDecimal orderAmount,
            String currency,
            String sku,
            Integer quantity,
            String status,
            String correlationId,
            List<OrderLine> orderLines
    ) implements Serializable {
        public OrderDetailsResponse(
                String orderId,
                String customerId,
                BigDecimal orderAmount,
                String currency,
                String sku,
                Integer quantity,
                String status,
                String correlationId
        ) {
            this(orderId, customerId, orderAmount, currency, sku, quantity, status, correlationId, null);
        }

        public List<OrderLine> resolvedOrderLines() {
            if (orderLines != null && !orderLines.isEmpty()) {
                return orderLines;
            }
            return List.of(new OrderLine(sku, quantity));
        }
    }

    record OrderLine(String sku, Integer quantity) implements Serializable {
    }
}
