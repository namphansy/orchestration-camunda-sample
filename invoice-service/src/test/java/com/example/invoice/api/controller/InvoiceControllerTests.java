package com.example.invoice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.invoice.domain.repository.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InvoiceControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Test
    void generatesInvoice() throws Exception {
        mockMvc.perform(post("/api/invoices")
                        .header("Idempotency-Key", "invoice-test-key-success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "invoice-order-test-1",
                                  "amount": 120.50,
                                  "currency": "USD",
                                  "correlationId": "correlation-invoice-test-1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("GENERATED"));
    }

    @Test
    void duplicateInvoiceRequestDoesNotCreateDuplicateInvoice() throws Exception {
        String requestBody = """
                {
                  "orderId": "invoice-order-test-2",
                  "amount": 75.00,
                  "currency": "USD",
                  "correlationId": "correlation-invoice-test-2"
                }
                """;

        mockMvc.perform(post("/api/invoices")
                        .header("Idempotency-Key", "invoice-test-key-duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("GENERATED"));

        mockMvc.perform(post("/api/invoices")
                        .header("Idempotency-Key", "invoice-test-key-duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("GENERATED"));

        assertThat(invoiceRepository.findAll())
                .filteredOn(invoice -> invoice.getOrderId().equals("invoice-order-test-2"))
                .hasSize(1);
    }
}
