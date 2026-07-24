package com.example.inventory.domain.repository;

import com.example.inventory.domain.model.StockItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockItemRepository extends JpaRepository<StockItem, String> {
}
