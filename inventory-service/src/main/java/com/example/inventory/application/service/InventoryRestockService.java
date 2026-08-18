package com.example.inventory.application.service;

import com.example.inventory.api.request.RestockInventoryRequest;
import com.example.inventory.api.response.InventoryRestockResponse;
import com.example.inventory.domain.model.InventoryRestock;
import com.example.inventory.domain.model.InventoryRestockStatus;
import com.example.inventory.domain.model.StockItem;
import com.example.inventory.domain.repository.InventoryRestockRepository;
import com.example.inventory.domain.repository.StockItemRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryRestockService {

    private final InventoryRestockRepository restockRepository;
    private final StockItemRepository stockItemRepository;

    public InventoryRestockService(
            InventoryRestockRepository restockRepository,
            StockItemRepository stockItemRepository
    ) {
        this.restockRepository = restockRepository;
        this.stockItemRepository = stockItemRepository;
    }

    @Transactional
    public InventoryRestockResponse restock(
            RestockInventoryRequest request,
            String idempotencyKey
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key header is required"
            );
        }

        InventoryRestock existingByKey = restockRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElse(null);

        if (existingByKey != null) {
            validateSameRequest(existingByKey, request);
            return InventoryRestockResponse.from(existingByKey);
        }

        InventoryRestock existingByReturnAndSku = restockRepository
                .findByReturnIdAndSku(request.returnId(), request.sku())
                .orElse(null);

        if (existingByReturnAndSku != null) {
            throw new IllegalStateException(
                    "Inventory was already restocked for return "
                            + request.returnId()
                            + " and SKU "
                            + request.sku()
            );
        }

        StockItem stockItem = stockItemRepository
                .findById(request.sku())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Stock item not found: " + request.sku()
                ));

        stockItem.release(request.quantity());

        InventoryRestock restock = new InventoryRestock(
                UUID.randomUUID().toString(),
                request.returnId(),
                request.orderId(),
                request.sku(),
                request.quantity(),
                InventoryRestockStatus.RESTOCKED,
                idempotencyKey,
                request.correlationId()
        );

        return InventoryRestockResponse.from(
                restockRepository.save(restock)
        );
    }

    private void validateSameRequest(
            InventoryRestock existing,
            RestockInventoryRequest request
    ) {
        boolean sameRequest =
                existing.getReturnId().equals(request.returnId())
                && existing.getOrderId().equals(request.orderId())
                && existing.getSku().equals(request.sku())
                && existing.getQuantity().equals(request.quantity())
                && existing.getCorrelationId()
                        .equals(request.correlationId());

        if (!sameRequest) {
            throw new IllegalStateException(
                    "Idempotency-Key was already used for another request"
            );
        }
    }
}