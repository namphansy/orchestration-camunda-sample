package com.example.inventory.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.inventory.domain.repository.StockItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StockItemRepository stockItemRepository;

    @Test
    void duplicateReservationRequestDoesNotDecrementStockTwice() throws Exception {
        String requestBody = """
                {
                  "orderId": "inventory-order-test-1",
                  "sku": "SKU-DEFAULT",
                  "quantity": 5,
                  "correlationId": "correlation-inventory-test-1"
                }
                """;

        mockMvc.perform(post("/api/inventory/reservations")
                        .header("Idempotency-Key", "inventory-test-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESERVED"));

        mockMvc.perform(post("/api/inventory/reservations")
                        .header("Idempotency-Key", "inventory-test-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESERVED"));

        Integer availableQuantity = stockItemRepository.findById("SKU-DEFAULT")
                .orElseThrow()
                .getAvailableQuantity();
        assertThat(availableQuantity).isEqualTo(95);
    }

    @Test
    void insufficientStockReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/inventory/reservations")
                        .header("Idempotency-Key", "inventory-test-key-insufficient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "inventory-order-test-2",
                                  "sku": "SKU-DEFAULT",
                                  "quantity": 1000,
                                  "correlationId": "correlation-inventory-test-2"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"));
    }
}
