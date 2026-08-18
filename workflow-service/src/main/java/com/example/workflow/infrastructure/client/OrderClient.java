package com.example.workflow.infrastructure.client;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.time.Instant;

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
            String correlationId,
            List<OrderLine> orderLines,
            Instant deliveredAt
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
            this(
                    orderId,
                    customerId,
                    orderAmount,
                    currency,
                    sku,
                    quantity,
                    status,
                    correlationId,
                    null,
                    null
            );
        }
        public OrderDetailsResponse(
                String orderId,
                String customerId,
                BigDecimal orderAmount,
                String currency,
                String sku,
                Integer quantity,
                String status,
                String correlationId,
                List<OrderLine> orderLines
        ) {
            this(
                    orderId,
                    customerId,
                    orderAmount,
                    currency,
                    sku,
                    quantity,
                    status,
                    correlationId,
                    orderLines,
                    null
            );
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
