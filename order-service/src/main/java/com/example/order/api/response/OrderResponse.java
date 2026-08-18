package com.example.order.api.response;

import com.example.order.domain.model.OrderEntity;
import com.example.order.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        String orderId,
        String customerId,
        BigDecimal orderAmount,
        String currency,
        String sku,
        Integer quantity,
        OrderStatus status,
        String correlationId,
        String workflowProcessInstanceId,
        Instant deliveredAt
) {
    public static OrderResponse from(OrderEntity order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getCustomerId(),
                order.getOrderAmount(),
                order.getCurrency(),
                order.getSku(),
                order.getQuantity(),
                order.getStatus(),
                order.getCorrelationId(),
                order.getWorkflowProcessInstanceId(),
                order.getDeliveredAt()
        );
    }
}
