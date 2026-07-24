package com.example.notification.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "notification.fail-order-ids[0]=notification-order-failure")
@AutoConfigureMockMvc
class NotificationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publishesNotification() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "notification-order-success",
                                  "customerId": "customer-1",
                                  "channel": "EMAIL",
                                  "message": "Order notification",
                                  "correlationId": "correlation-notification-success"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void configuredNotificationFailureReturnsServiceUnavailable() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "notification-order-failure",
                                  "customerId": "customer-1",
                                  "channel": "EMAIL",
                                  "message": "Order notification",
                                  "correlationId": "correlation-notification-failure"
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("NOTIFICATION_PUBLISH_FAILED"));
    }
}
