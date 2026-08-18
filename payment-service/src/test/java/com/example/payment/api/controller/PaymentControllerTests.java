package com.example.payment.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.payment.domain.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentTransactionRepository transactionRepository;

    @Test
    void successfulChargeCreatesTransaction() throws Exception {
        mockMvc.perform(post("/api/payments/charges")
                        .header("Idempotency-Key", "payment-test-key-success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "payment-order-test-1",
                                  "amount": 120.50,
                                  "currency": "USD",
                                  "correlationId": "correlation-payment-test-1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CHARGED"));
    }

    @Test
    void declineScenarioCreatesDeclinedTransaction() throws Exception {
        mockMvc.perform(post("/api/payments/charges")
                        .header("Idempotency-Key", "payment-test-key-decline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "payment-order-test-2",
                                  "amount": 120.50,
                                  "currency": "USD",
                                  "correlationId": "correlation-payment-test-2",
                                  "simulation": "DECLINE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DECLINED"));
    }

    @Test
    void duplicateChargeRequestDoesNotCreateDuplicateTransaction() throws Exception {
        String requestBody = """
                {
                  "orderId": "payment-order-test-3",
                  "amount": 75.00,
                  "currency": "USD",
                  "correlationId": "correlation-payment-test-3"
                }
                """;

        mockMvc.perform(post("/api/payments/charges")
                        .header("Idempotency-Key", "payment-test-key-duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CHARGED"));

        mockMvc.perform(post("/api/payments/charges")
                        .header("Idempotency-Key", "payment-test-key-duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CHARGED"));

        assertThat(transactionRepository.findAll())
                .filteredOn(transaction -> transaction.getOrderId().equals("payment-order-test-3"))
                .hasSize(1);
    }

    @Test
    void duplicateRefundRequestDoesNotCreateDuplicateRefund() throws Exception {
        String requestBody = """
                {
                  "orderId": "payment-order-test-refund",
                  "amount": 90.00,
                  "currency": "USD",
                  "correlationId": "correlation-payment-test-refund"
                }
                """;

        String transactionId = mockMvc.perform(post("/api/payments/charges")
                        .header("Idempotency-Key", "payment-test-key-refund-charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CHARGED"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"transactionId\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/payments/transactions/{transactionId}/refund", transactionId)
                        .header("Idempotency-Key", "payment-test-key-refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        mockMvc.perform(post("/api/payments/transactions/{transactionId}/refund", transactionId)
                        .header("Idempotency-Key", "payment-test-key-refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        assertThat(transactionRepository.findAll())
                .filteredOn(transaction -> transaction.getOrderId().equals("payment-order-test-refund"))
                .hasSize(1)
                .allSatisfy(transaction -> assertThat(transaction.getStatus().name()).isEqualTo("REFUNDED"));
    }

    @Test
    void technicalFailureSimulationReturnsServiceUnavailableWithoutTransaction() throws Exception {
        mockMvc.perform(post("/api/payments/charges")
                        .header("Idempotency-Key", "payment-test-key-failure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "payment-order-test-4",
                                  "amount": 120.50,
                                  "currency": "USD",
                                  "correlationId": "correlation-payment-test-4",
                                  "simulation": "TECHNICAL_FAILURE"
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("PAYMENT_TECHNICAL_FAILURE"));

        assertThat(transactionRepository.findAll())
                .filteredOn(transaction -> transaction.getOrderId().equals("payment-order-test-4"))
                .isEmpty();
    }

    @Test
    void findsChargedTransactionByOrderId() throws Exception {
        mockMvc.perform(post("/api/payments/charges")
                        .header(
                                "Idempotency-Key",
                                "payment-test-key-order-lookup"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "orderId": "payment-order-lookup",
                                "amount": 45.00,
                                "currency": "USD",
                                "correlationId": "correlation-payment-order-lookup"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CHARGED"));

        mockMvc.perform(get(
                        "/api/payments/transactions/by-order/{orderId}",
                        "payment-order-lookup"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId")
                        .value("payment-order-lookup"))
                .andExpect(jsonPath("$.amount").value(45.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("CHARGED"))
                .andExpect(jsonPath("$.transactionId").isNotEmpty());
    }
}
