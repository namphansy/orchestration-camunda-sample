package com.example.inventory.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_items")
public class StockItem {

    @Id
    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    protected StockItem() {
    }

    public StockItem(String sku, Integer availableQuantity) {
        this.sku = sku;
        this.availableQuantity = availableQuantity;
    }

    public void reserve(Integer quantity) {
        availableQuantity -= quantity;
    }

    public void release(Integer quantity) {
        availableQuantity += quantity;
    }

    public String getSku() {
        return sku;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }
}
