package com.example.workflow.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.example.workflow.infrastructure.client.InventoryClient;
import com.example.workflow.infrastructure.client.InvoiceClient;
import com.example.workflow.infrastructure.client.NotificationClient;
import com.example.workflow.infrastructure.client.OrderClient;
import com.example.workflow.infrastructure.client.PaymentClient;
import com.example.workflow.infrastructure.client.ShippingClient;
import com.example.workflow.shared.ProcessVariables;
import java.math.BigDecimal;
import java.util.Map;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "camunda.bpm.job-execution.enabled=false")
@AutoConfigureMockMvc
class OrderWorkflowControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ManagementService managementService;

    @Autowired
    private TaskService taskService;

    @MockBean
    private InventoryClient inventoryClient;

    @MockBean
    private OrderClient orderClient;

    @MockBean
    private PaymentClient paymentClient;

    @MockBean
    private ShippingClient shippingClient;

    @MockBean
    private InvoiceClient invoiceClient;

    @MockBean
    private NotificationClient notificationClient;

    @Test
    void startsOrderWorkflowThroughRest() throws Exception {
        when(inventoryClient.reserveInventory(any(), eq("inventory-reservation:order-rest-test-1")))
                .thenReturn(new InventoryClient.InventoryReservationResponse(
                        "reservation-rest-test-1",
                        "order-rest-test-1",
                        "SKU-DEFAULT",
                        1,
                        "RESERVED",
                        "correlation-rest-test-1"
                ));
        when(orderClient.getOrder("order-rest-test-1"))
                .thenReturn(new OrderClient.OrderDetailsResponse(
                        "order-rest-test-1",
                        "customer-rest-1",
                        new BigDecimal("250.75"),
                        "USD",
                        "SKU-DEFAULT",
                        1,
                        "PROCESSING",
                        "correlation-rest-test-1"
                ));

        mockMvc.perform(post("/api/workflows/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "order-rest-test-1",
                                  "correlationId": "correlation-rest-test-1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.businessKey").value("order-rest-test-1"))
                .andExpect(jsonPath("$.correlationId").value("correlation-rest-test-1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void listsClaimsAndCompletesApprovalTaskThroughRest() throws Exception {
        String businessKey = "order-rest-test-manager-approval";
        String correlationId = "correlation-rest-test-manager-approval";
        when(inventoryClient.reserveInventory(any(), eq("inventory-reservation:" + businessKey)))
                .thenReturn(new InventoryClient.InventoryReservationResponse(
                        "reservation-" + businessKey,
                        businessKey,
                        "SKU-DEFAULT",
                        1,
                        "RESERVED",
                        correlationId
                ));
        when(orderClient.getOrder(businessKey))
                .thenReturn(new OrderClient.OrderDetailsResponse(
                        businessKey,
                        "customer-rest-1",
                        new BigDecimal("6000.00"),
                        "USD",
                        "SKU-DEFAULT",
                        1,
                        "PROCESSING",
                        correlationId
                ));
        when(paymentClient.chargePayment(any(), eq("payment-charge:" + businessKey)))
                .thenReturn(new PaymentClient.PaymentChargeResponse(
                        "payment-" + businessKey,
                        businessKey,
                        new BigDecimal("6000.00"),
                        "USD",
                        "CHARGED",
                        correlationId,
                        null
                ));
        when(shippingClient.createShipment(any(), eq("shipment-creation:" + businessKey)))
                .thenReturn(new ShippingClient.ShipmentResponse(
                        "shipment-" + businessKey,
                        businessKey,
                        "SKU-DEFAULT",
                        1,
                        "CREATED",
                        correlationId,
                        null
                ));
        when(invoiceClient.generateInvoice(any(), eq("invoice-generation:" + businessKey)))
                .thenReturn(new InvoiceClient.InvoiceResponse(
                        "invoice-" + businessKey,
                        businessKey,
                        new BigDecimal("6000.00"),
                        "USD",
                        "GENERATED",
                        correlationId
                ));
        when(notificationClient.publishNotification(any()))
                .thenReturn(new NotificationClient.NotificationResponse(
                        "notification-" + businessKey,
                        businessKey,
                        "PUBLISHED",
                        correlationId
                ));

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "order-processing",
                businessKey,
                Map.of(
                        ProcessVariables.ORDER_ID, businessKey,
                        ProcessVariables.BUSINESS_KEY, businessKey,
                        ProcessVariables.CORRELATION_ID, correlationId,
                        ProcessVariables.ORDER_STATUS, "CREATED"
                )
        );
        Job paymentJob = singlePaymentJob(businessKey);
        managementService.executeJob(paymentJob.getId());
        confirmPaymentThroughRest(businessKey, correlationId, "payment-" + businessKey, "CHARGED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CORRELATED"));
        Task approvalTask = taskService.createTaskQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .taskDefinitionKey("ManagerApprovalTask")
                .singleResult();

        mockMvc.perform(get("/api/workflows/orders/tasks")
                        .param("candidateGroup", "managers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").value(approvalTask.getId()))
                .andExpect(jsonPath("$[0].approvalLevel").value("MANAGER"))
                .andExpect(jsonPath("$[0].candidateGroup").value("managers"));

        mockMvc.perform(post("/api/workflows/orders/tasks/{taskId}/claim", approvalTask.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignee": "manager-rest-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").value("manager-rest-1"));

        mockMvc.perform(post("/api/workflows/orders/tasks/{taskId}/complete", approvalTask.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approved": true,
                                  "approver": "manager-rest-1"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void confirmsPaymentThroughRestAndSafelyIgnoresDuplicatesOrUnknownMessages() throws Exception {
        String businessKey = "order-rest-test-payment-confirmation";
        String correlationId = "correlation-rest-test-payment-confirmation";
        when(inventoryClient.reserveInventory(any(), eq("inventory-reservation:" + businessKey)))
                .thenReturn(new InventoryClient.InventoryReservationResponse(
                        "reservation-" + businessKey,
                        businessKey,
                        "SKU-DEFAULT",
                        1,
                        "RESERVED",
                        correlationId
                ));
        when(orderClient.getOrder(businessKey))
                .thenReturn(new OrderClient.OrderDetailsResponse(
                        businessKey,
                        "customer-rest-1",
                        new BigDecimal("120.50"),
                        "USD",
                        "SKU-DEFAULT",
                        1,
                        "PROCESSING",
                        correlationId
                ));
        when(paymentClient.chargePayment(any(), eq("payment-charge:" + businessKey)))
                .thenReturn(new PaymentClient.PaymentChargeResponse(
                        "payment-" + businessKey,
                        businessKey,
                        new BigDecimal("120.50"),
                        "USD",
                        "CHARGED",
                        correlationId,
                        null
                ));
        when(shippingClient.createShipment(any(), eq("shipment-creation:" + businessKey)))
                .thenReturn(new ShippingClient.ShipmentResponse(
                        "shipment-" + businessKey,
                        businessKey,
                        "SKU-DEFAULT",
                        1,
                        "CREATED",
                        correlationId,
                        null
                ));
        when(invoiceClient.generateInvoice(any(), eq("invoice-generation:" + businessKey)))
                .thenReturn(new InvoiceClient.InvoiceResponse(
                        "invoice-" + businessKey,
                        businessKey,
                        new BigDecimal("120.50"),
                        "USD",
                        "GENERATED",
                        correlationId
                ));
        when(notificationClient.publishNotification(any()))
                .thenReturn(new NotificationClient.NotificationResponse(
                        "notification-" + businessKey,
                        businessKey,
                        "PUBLISHED",
                        correlationId
                ));

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "order-processing",
                businessKey,
                Map.of(
                        ProcessVariables.ORDER_ID, businessKey,
                        ProcessVariables.BUSINESS_KEY, businessKey,
                        ProcessVariables.CORRELATION_ID, correlationId,
                        ProcessVariables.ORDER_STATUS, "CREATED"
                )
        );
        managementService.executeJob(singlePaymentJob(businessKey).getId());

        confirmPaymentThroughRest(businessKey, "wrong-correlation", "payment-" + businessKey, "CHARGED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IGNORED"));

        confirmPaymentThroughRest(businessKey, correlationId, "payment-" + businessKey, "CHARGED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CORRELATED"));

        confirmPaymentThroughRest(businessKey, correlationId, "payment-" + businessKey, "CHARGED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IGNORED"));

        mockMvc.perform(post("/api/workflows/orders/unknown-order/payment-confirmations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correlationId": "missing-correlation",
                                  "paymentTransactionId": "missing-payment",
                                  "paymentStatus": "CHARGED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IGNORED"));
    }

    private org.springframework.test.web.servlet.ResultActions confirmPaymentThroughRest(
            String businessKey,
            String correlationId,
            String paymentTransactionId,
            String paymentStatus
    ) throws Exception {
        return mockMvc.perform(post("/api/workflows/orders/{businessKey}/payment-confirmations", businessKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "correlationId": "%s",
                          "paymentTransactionId": "%s",
                          "paymentStatus": "%s"
                        }
                        """.formatted(correlationId, paymentTransactionId, paymentStatus)));
    }

    private Job singlePaymentJob(String businessKey) {
        return managementService.createJobQuery()
                .activityId("ChargePayment")
                .list()
                .stream()
                .filter(job -> businessKey.equals(runtimeService.getVariable(
                        job.getProcessInstanceId(),
                        ProcessVariables.BUSINESS_KEY
                )))
                .findFirst()
                .orElse(null);
    }

}
