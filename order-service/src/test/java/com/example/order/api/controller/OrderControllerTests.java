package com.example.order.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.order.infrastructure.client.WorkflowClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkflowClient workflowClient;

    @Test
    void createsOrderAndStartsWorkflow() throws Exception {
        when(workflowClient.startOrderWorkflow(any(), eq("correlation-order-test-1")))
                .thenReturn(new WorkflowClient.WorkflowStartResponse(
                        "process-order-test-1",
                        "order-test-1",
                        "correlation-order-test-1",
                        "COMPLETED"
                ));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "order-test-1",
                                  "customerId": "customer-test-1",
                                  "orderAmount": 42.00,
                                  "currency": "USD",
                                  "sku": "SKU-DEFAULT",
                                  "quantity": 2,
                                  "correlationId": "correlation-order-test-1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("order-test-1"))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.workflowProcessInstanceId").value("process-order-test-1"));
    }
}
