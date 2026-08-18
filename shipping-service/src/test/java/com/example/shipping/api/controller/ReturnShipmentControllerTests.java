package com.example.shipping.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.shipping.domain.repository.ReturnShipmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReturnShipmentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReturnShipmentRepository returnShipmentRepository;

    @Test
    void createsAndGetsReturnShipment() throws Exception {
        String responseBody = mockMvc.perform(post("/api/return-shipments")
                        .header(
                                "Idempotency-Key",
                                "return-shipment:return-test-1"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "returnId": "return-test-1",
                                  "orderId": "order-test-1",
                                  "customerId": "customer-test-1",
                                  "correlationId": "correlation-return-test-1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.returnId")
                        .value("return-test-1"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String returnShipmentId = responseBody.replaceAll(
                ".*\"returnShipmentId\":\"([^\"]+)\".*",
                "$1"
        );

        mockMvc.perform(get(
                        "/api/return-shipments/{returnShipmentId}",
                        returnShipmentId
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnId")
                        .value("return-test-1"))
                .andExpect(jsonPath("$.orderId")
                        .value("order-test-1"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void duplicateRequestDoesNotCreateSecondReturnShipment()
            throws Exception {
        String requestBody = """
                {
                  "returnId": "return-test-duplicate",
                  "orderId": "order-test-duplicate",
                  "customerId": "customer-test-duplicate",
                  "correlationId": "correlation-return-duplicate"
                }
                """;

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/return-shipments")
                            .header(
                                    "Idempotency-Key",
                                    "return-shipment:return-test-duplicate"
                            )
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("CREATED"));
        }

        assertThat(returnShipmentRepository.findByReturnId(
                "return-test-duplicate"
        )).isPresent();

        assertThat(returnShipmentRepository.findAll())
                .filteredOn(returnShipment ->
                        returnShipment.getReturnId()
                                .equals("return-test-duplicate"))
                .hasSize(1);
    }
}