package com.example.inventory.api.response;

import com.example.inventory.domain.model.InventoryRestock;
import com.example.inventory.domain.model.InventoryRestockStatus;
import java.time.Instant;

public record InventoryRestockResponse(
        String restockId,
        String returnId,
        String orderId,
        String sku,
        Integer quantity,
        InventoryRestockStatus status,
        String correlationId,
        Instant createdAt
) {
    public static InventoryRestockResponse from(InventoryRestock restock) {
        return new InventoryRestockResponse(
                restock.getRestockId(),
                restock.getReturnId(),
                restock.getOrderId(),
                restock.getSku(),
                restock.getQuantity(),
                restock.getStatus(),
                restock.getCorrelationId(),
                restock.getCreatedAt()
        );
    }
}
