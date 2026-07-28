package com.example.shipping.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.shipping.domain.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ShippingControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Test
    void duplicateShipmentRequestDoesNotCreateDuplicateShipment() throws Exception {
        String requestBody = """
                {
                  "orderId": "shipping-order-test-1",
                  "sku": "SKU-DEFAULT",
                  "quantity": 2,
                  "correlationId": "correlation-shipping-test-1"
                }
                """;

        mockMvc.perform(post("/api/shipments")
                        .header("Idempotency-Key", "shipping-test-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"));

        mockMvc.perform(post("/api/shipments")
                        .header("Idempotency-Key", "shipping-test-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"));

        assertThat(shipmentRepository.findAll())
                .filteredOn(shipment -> shipment.getOrderId().equals("shipping-order-test-1"))
                .hasSize(1);
    }

    @Test
    void carrierFailureCreatesFailedShipment() throws Exception {
        mockMvc.perform(post("/api/shipments")
                        .header("Idempotency-Key", "shipping-test-key-failure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "shipping-order-test-2",
                                  "sku": "SKU-DEFAULT",
                                  "quantity": 1,
                                  "correlationId": "correlation-shipping-test-2",
                                  "simulation": "CARRIER_FAILURE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("Carrier rejected shipment creation"));
    }
}
