package com.example.inventory.domain.repository;

import com.example.inventory.domain.model.InventoryRestock;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRestockRepository
        extends JpaRepository<InventoryRestock, String> {

    Optional<InventoryRestock> findByIdempotencyKey(
            String idempotencyKey
    );

    Optional<InventoryRestock> findByReturnIdAndSku(
            String returnId,
            String sku
    );
}