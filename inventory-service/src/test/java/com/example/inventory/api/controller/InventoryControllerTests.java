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
        Integer startingQuantity = stockItemRepository.findById("SKU-DEFAULT")
                .orElseThrow()
                .getAvailableQuantity();

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
        assertThat(availableQuantity).isEqualTo(startingQuantity - 5);
    }

    @Test
    void duplicateReleaseRequestDoesNotIncrementStockTwice() throws Exception {
        String requestBody = """
                {
                  "orderId": "inventory-order-test-release",
                  "sku": "SKU-DEFAULT",
                  "quantity": 4,
                  "correlationId": "correlation-inventory-test-release"
                }
                """;
        Integer startingQuantity = stockItemRepository.findById("SKU-DEFAULT")
                .orElseThrow()
                .getAvailableQuantity();

        String reservationId = mockMvc.perform(post("/api/inventory/reservations")
                        .header("Idempotency-Key", "inventory-test-key-release-reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"reservationId\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/inventory/reservations/{reservationId}/release", reservationId)
                        .header("Idempotency-Key", "inventory-test-key-release"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"));

        mockMvc.perform(post("/api/inventory/reservations/{reservationId}/release", reservationId)
                        .header("Idempotency-Key", "inventory-test-key-release"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"));

        Integer availableQuantity = stockItemRepository.findById("SKU-DEFAULT")
                .orElseThrow()
                .getAvailableQuantity();
        assertThat(availableQuantity).isEqualTo(startingQuantity);
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

    @Test
    void restockAddsStockOnlyOnceForDuplicateRequest()
                throws Exception {
        int quantityBefore = stockItemRepository
                .findById("SKU-DEFAULT")
                .orElseThrow()
                .getAvailableQuantity();

        String requestBody = """
                {
                "returnId": "return-restock-test-1",
                "orderId": "order-restock-test-1",
                "sku": "SKU-DEFAULT",
                "quantity": 2,
                "correlationId": "correlation-restock-test-1"
                }
                """;

        for (int attempt = 0; attempt < 2; attempt++) {
                mockMvc.perform(post("/api/inventory/restocks")
                                .header(
                                        "Idempotency-Key",
                                        "inventory-restock:"
                                                + "return-restock-test-1:"
                                                + "SKU-DEFAULT"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status")
                                .value("RESTOCKED"))
                        .andExpect(jsonPath("$.quantity").value(2));
        }

        int quantityAfter = stockItemRepository
                .findById("SKU-DEFAULT")
                .orElseThrow()
                .getAvailableQuantity();

        assertThat(quantityAfter).isEqualTo(quantityBefore + 2);
    }
}
