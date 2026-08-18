package com.example.workflow.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import com.example.workflow.infrastructure.client.NotificationClient;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.camunda.bpm.engine.runtime.Incident;
import static org.mockito.Mockito.verify;
import com.example.workflow.infrastructure.client.InventoryClient;
import com.example.workflow.api.request.ReturnItemReceivedRequest;
import com.example.workflow.api.response.ReturnEventResponse;
import com.example.workflow.api.request.StartReturnWorkflowRequest;
import com.example.workflow.api.response.ReturnWorkflowResponse;
import com.example.workflow.infrastructure.client.OrderClient;
import com.example.workflow.infrastructure.client.ReturnShippingClient;
import com.example.workflow.api.request.ReturnInspectionItemRequest;
import com.example.workflow.api.request.ReturnInspectionRequest;
import java.util.List;
import java.math.BigDecimal;
import java.time.Instant;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.camunda.bpm.engine.RepositoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.workflow.infrastructure.client.PaymentClient;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.runtime.Job;

@SpringBootTest(properties = "camunda.bpm.job-execution.enabled=false")
class ReturnWorkflowProcessTests {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final ReturnWorkflowService returnWorkflowService;
    private final ManagementService managementService;

    @MockBean
    private PaymentClient paymentClient;

    @MockBean
    private OrderClient orderClient;

    @MockBean
    private ReturnShippingClient returnShippingClient;

    @MockBean
    private InventoryClient inventoryClient;

    @MockBean
    private NotificationClient notificationClient;

    @Autowired
    ReturnWorkflowProcessTests(
            RepositoryService repositoryService,
            RuntimeService runtimeService,
            ReturnWorkflowService returnWorkflowService,
            ManagementService managementService
    ) {
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.returnWorkflowService = returnWorkflowService;
        this.managementService = managementService;
    }

    @Test
    void deploysReturnRefundProcess() {
        long deployedDefinitions =
                repositoryService
                        .createProcessDefinitionQuery()
                        .processDefinitionKey(
                                ReturnWorkflowService.PROCESS_DEFINITION_KEY
                        )
                        .count();

        assertThat(deployedDefinitions).isPositive();
    }

    @Test
    void startsReturnAndWaitsForReturnedItem() {
        String returnId = "return-test-1001";
        String orderId = "order-test-1001";
        String customerId = "customer-42";
        String correlationId = "correlation-return-1001";

        when(orderClient.getOrder(orderId))
                .thenReturn(new OrderClient.OrderDetailsResponse(
                        orderId,
                        customerId,
                        new BigDecimal("120.50"),
                        "USD",
                        "SKU-DEFAULT",
                        1,
                        "COMPLETED",
                        correlationId,
                        null,
                        Instant.now().minusSeconds(24 * 60 * 60)
                ));

        when(returnShippingClient.createReturnShipment(
                any(ReturnShippingClient.CreateReturnShipmentRequest.class),
                eq("return-shipment:" + returnId)
        )).thenReturn(
                new ReturnShippingClient.ReturnShipmentResponse(
                        "return-shipment-test-1001",
                        returnId,
                        orderId,
                        customerId,
                        "CREATED",
                        correlationId
                )
        );

        ReturnWorkflowResponse response =
                returnWorkflowService.startReturnWorkflow(
                        new StartReturnWorkflowRequest(
                                returnId,
                                orderId,
                                customerId,
                                "Product is defective",
                                correlationId
                        )
                );

        assertThat(response.returnId()).isEqualTo(returnId);
        assertThat(response.returnStatus())
                .isEqualTo("WAITING_FOR_ITEM");

        assertThat(
                runtimeService.createExecutionQuery()
                        .processInstanceBusinessKey(returnId)
                        .activityId("WaitForReturnItem")
                        .singleResult()
        ).isNotNull();
        ReturnEventResponse ignoredResponse =
        returnWorkflowService.receiveReturnItem(
                returnId,
                new ReturnItemReceivedRequest(
                        "warehouse-user",
                        Instant.now(),
                        "wrong-correlation-id"
                )
        );

assertThat(ignoredResponse.status())
        .isEqualTo("IGNORED");

assertThat(
        runtimeService.createExecutionQuery()
                .processInstanceBusinessKey(returnId)
                .activityId("WaitForReturnItem")
                .singleResult()
).isNotNull();

assertThat(
        runtimeService.createExecutionQuery()
                .processInstanceBusinessKey(returnId)
                .activityId("WaitForInspection")
                .singleResult()
).isNull();
    }

    @Test
    void receivedEventMovesReturnToInspection() {
        String returnId = "return-test-1002";
        String orderId = "order-test-1002";
        String customerId = "customer-42";
        String correlationId = "correlation-return-1002";

        when(orderClient.getOrder(orderId))
                .thenReturn(new OrderClient.OrderDetailsResponse(
                        orderId,
                        customerId,
                        new BigDecimal("120.50"),
                        "USD",
                        "SKU-DEFAULT",
                        1,
                        "COMPLETED",
                        correlationId,
                        null,
                        Instant.now().minusSeconds(24 * 60 * 60)
                ));

        when(returnShippingClient.createReturnShipment(
                any(ReturnShippingClient.CreateReturnShipmentRequest.class),
                eq("return-shipment:" + returnId)
        )).thenReturn(
                new ReturnShippingClient.ReturnShipmentResponse(
                        "return-shipment-test-1002",
                        returnId,
                        orderId,
                        customerId,
                        "CREATED",
                        correlationId
                )
        );

        returnWorkflowService.startReturnWorkflow(
                new StartReturnWorkflowRequest(
                        returnId,
                        orderId,
                        customerId,
                        "Product is defective",
                        correlationId
                )
        );

        ReturnEventResponse eventResponse =
                returnWorkflowService.receiveReturnItem(
                        returnId,
                        new ReturnItemReceivedRequest(
                                "warehouse-user",
                                Instant.now(),
                                correlationId
                        )
                );

        assertThat(eventResponse.status()).isEqualTo("CORRELATED");

        assertThat(
                runtimeService.createExecutionQuery()
                        .processInstanceBusinessKey(returnId)
                        .activityId("WaitForInspection")
                        .singleResult()
        ).isNotNull();

        assertThat(
                runtimeService.createVariableInstanceQuery()
                        .processInstanceIdIn(
                                runtimeService
                                        .createProcessInstanceQuery()
                                        .processInstanceBusinessKey(returnId)
                                        .singleResult()
                                        .getId()
                        )
                        .variableName("returnStatus")
                        .singleResult()
                        .getValue()
        ).isEqualTo("INSPECTION");
    }

    @Test
    void rejectedInspectionMovesReturnToRejectedNotification() {
        String returnId = "return-test-1003";
        String orderId = "order-test-1003";
        String customerId = "customer-42";
        String correlationId = "correlation-return-1003";

        when(orderClient.getOrder(orderId))
                .thenReturn(new OrderClient.OrderDetailsResponse(
                        orderId,
                        customerId,
                        new BigDecimal("120.50"),
                        "USD",
                        "SKU-DEFAULT",
                        1,
                        "COMPLETED",
                        correlationId,
                        null,
                        Instant.now().minusSeconds(24 * 60 * 60)
                ));

        when(returnShippingClient.createReturnShipment(
                any(ReturnShippingClient.CreateReturnShipmentRequest.class),
                eq("return-shipment:" + returnId)
        )).thenReturn(
                new ReturnShippingClient.ReturnShipmentResponse(
                        "return-shipment-test-1003",
                        returnId,
                        orderId,
                        customerId,
                        "CREATED",
                        correlationId
                )
        );

        ReturnWorkflowResponse started =
                returnWorkflowService.startReturnWorkflow(
                        new StartReturnWorkflowRequest(
                                returnId,
                                orderId,
                                customerId,
                                "Product is defective",
                                correlationId
                        )
                );

        returnWorkflowService.receiveReturnItem(
                returnId,
                new ReturnItemReceivedRequest(
                        "warehouse-user",
                        Instant.now(),
                        correlationId
                )
        );

        ReturnEventResponse inspectionResponse =
                returnWorkflowService.completeInspection(
                        returnId,
                        new ReturnInspectionRequest(
                                false,
                                "inspector-user",
                                "Product is damaged",
                                List.of(
                                        new ReturnInspectionItemRequest(
                                                "SKU-DEFAULT",
                                                1,
                                                false
                                        )
                                ),
                                correlationId
                        )
                );

        assertThat(inspectionResponse.status())
                .isEqualTo("CORRELATED");

        assertThat(
                runtimeService.getVariable(
                        started.processInstanceId(),
                        "returnStatus"
                )
        ).isEqualTo("REJECTED");

        assertThat(
                runtimeService.createExecutionQuery()
                        .processInstanceId(started.processInstanceId())
                        .activityId("NotifyReturnRejected")
                        .singleResult()
        ).isNotNull();
    }

    @Test
    void acceptedInspectionKeepsAcceptedStatusBeforeRefundRuns() {
        String returnId = "return-test-1004";
        String orderId = "order-test-1004";
        String customerId = "customer-42";
        String correlationId = "correlation-return-1004";

        when(orderClient.getOrder(orderId))
                .thenReturn(new OrderClient.OrderDetailsResponse(
                        orderId,
                        customerId,
                        new BigDecimal("120.50"),
                        "USD",
                        "SKU-DEFAULT",
                        1,
                        "COMPLETED",
                        correlationId,
                        null,
                        Instant.now().minusSeconds(24 * 60 * 60)
                ));

        when(returnShippingClient.createReturnShipment(
                any(ReturnShippingClient.CreateReturnShipmentRequest.class),
                eq("return-shipment:" + returnId)
        )).thenReturn(
                new ReturnShippingClient.ReturnShipmentResponse(
                        "return-shipment-test-1004",
                        returnId,
                        orderId,
                        customerId,
                        "CREATED",
                        correlationId
                )
        );

        ReturnWorkflowResponse started =
                returnWorkflowService.startReturnWorkflow(
                        new StartReturnWorkflowRequest(
                                returnId,
                                orderId,
                                customerId,
                                "Product is defective",
                                correlationId
                        )
                );

        returnWorkflowService.receiveReturnItem(
                returnId,
                new ReturnItemReceivedRequest(
                        "warehouse-user",
                        Instant.now(),
                        correlationId
                )
        );

        ReturnEventResponse inspectionResponse =
                returnWorkflowService.completeInspection(
                        returnId,
                        new ReturnInspectionRequest(
                                true,
                                "inspector-user",
                                "Return accepted",
                                List.of(
                                        new ReturnInspectionItemRequest(
                                                "SKU-DEFAULT",
                                                1,
                                                true
                                        )
                                ),
                                correlationId
                        )
                );

        assertThat(inspectionResponse.status())
                .isEqualTo("CORRELATED");

        assertThat(
                runtimeService.getVariable(
                        started.processInstanceId(),
                        "returnStatus"
                )
        ).isEqualTo("ACCEPTED");

        assertThat(
                runtimeService.createExecutionQuery()
                        .processInstanceId(started.processInstanceId())
                        .activityId("RefundPayment")
                        .singleResult()
        ).isNotNull();

        when(paymentClient.getChargedTransactionByOrderId(orderId))
            .thenReturn(new PaymentClient.PaymentChargeResponse(
                    "payment-transaction-1004",
                    orderId,
                    new BigDecimal("120.50"),
                    "USD",
                    "CHARGED",
                    correlationId,
                    null
            ));

    when(paymentClient.refundPayment(
            "payment-transaction-1004",
            "return-refund:" + returnId
    )).thenReturn(new PaymentClient.PaymentChargeResponse(
            "refund-transaction-1004",
            orderId,
            new BigDecimal("120.50"),
            "USD",
            "REFUNDED",
            correlationId,
            null
    ));

    Job refundJob =
            managementService.createJobQuery()
                    .processInstanceId(started.processInstanceId())
                    .activityId("RefundPayment")
                    .singleResult();

    assertThat(refundJob).isNotNull();

    managementService.executeJob(refundJob.getId());

    assertThat(
            runtimeService.getVariable(
                    started.processInstanceId(),
                    "returnStatus"
            )
    ).isEqualTo("REFUNDED");

    assertThat(
            runtimeService.getVariable(
                    started.processInstanceId(),
                    "refundTransactionId"
            )
    ).isEqualTo("refund-transaction-1004");

    assertThat(
            runtimeService.createExecutionQuery()
                    .processInstanceId(started.processInstanceId())
                    .activityId("RestockInventory")
                    .singleResult()
    ).isNotNull();

    when(inventoryClient.restockInventory(
            eq(new InventoryClient.InventoryRestockRequest(
                    returnId,
                    orderId,
                    "SKU-DEFAULT",
                    1,
                    correlationId
            )),
            eq("inventory-restock:" + returnId + ":SKU-DEFAULT")
    )).thenReturn(new InventoryClient.InventoryRestockResponse(
            "restock-1004",
            returnId,
            orderId,
            "SKU-DEFAULT",
            1,
            "RESTOCKED",
            correlationId
    ));

    Job restockJob =
            managementService.createJobQuery()
                    .processInstanceId(started.processInstanceId())
                    .activityId("RestockInventory")
                    .singleResult();

    assertThat(restockJob).isNotNull();

    managementService.executeJob(restockJob.getId());

    assertThat(
            runtimeService.getVariable(
                    started.processInstanceId(),
                    "returnStatus"
            )
    ).isEqualTo("REFUNDED");

    assertThat(
            runtimeService.getVariable(
                    started.processInstanceId(),
                    "restockStatus"
            )
    ).isEqualTo("RESTOCKED");

    assertThat(
            runtimeService.createExecutionQuery()
                    .processInstanceId(started.processInstanceId())
                    .activityId("NotifyReturnRefunded")
                    .singleResult()
    ).isNotNull();

    when(notificationClient.publishNotification(
            any(NotificationClient.PublishNotificationRequest.class)
    )).thenReturn(new NotificationClient.NotificationResponse(
            "notification-1004",
            orderId,
            "PUBLISHED",
            correlationId
    ));

    Job notificationJob =
            managementService.createJobQuery()
                    .processInstanceId(started.processInstanceId())
                    .activityId("NotifyReturnRefunded")
                    .singleResult();

    assertThat(notificationJob).isNotNull();

    managementService.executeJob(notificationJob.getId());

    assertThat(
            runtimeService.createProcessInstanceQuery()
                    .processInstanceId(started.processInstanceId())
                    .singleResult()
    ).isNull();

    verify(notificationClient).publishNotification(
            eq(new NotificationClient.PublishNotificationRequest(
                    orderId,
                    customerId,
                    "EMAIL",
                    "Return " + returnId
                            + " was refunded successfully.",
                    correlationId
            ))
    );

    }

    @Test
    void refundTechnicalFailureRetriesAndCreatesIncident() {
        String returnId = "return-test-1005";
        String orderId = "order-test-1005";
        String customerId = "customer-42";
        String correlationId = "correlation-return-1005";

        ReturnWorkflowResponse started = moveReturnToAccepted(
                returnId,
                orderId,
                customerId,
                correlationId
        );

        when(paymentClient.getChargedTransactionByOrderId(orderId))
                .thenReturn(new PaymentClient.PaymentChargeResponse(
                        "payment-transaction-1005",
                        orderId,
                        new BigDecimal("120.50"),
                        "USD",
                        "CHARGED",
                        correlationId,
                        null
                ));

        when(paymentClient.refundPayment(
                "payment-transaction-1005",
                "return-refund:" + returnId
        )).thenThrow(new IllegalStateException(
                "Payment service unavailable"
        ));

        Job refundJob = managementService.createJobQuery()
                .processInstanceId(started.processInstanceId())
                .activityId("RefundPayment")
                .singleResult();

        assertThat(refundJob.getRetries()).isEqualTo(3);

        for (int expectedRetries = 2;
            expectedRetries >= 0;
            expectedRetries--) {

            String jobId = refundJob.getId();

            assertThatThrownBy(
                    () -> managementService.executeJob(jobId)
            ).isInstanceOf(RuntimeException.class);

            refundJob = managementService.createJobQuery()
                    .jobId(jobId)
                    .singleResult();

            assertThat(refundJob.getRetries())
                    .isEqualTo(expectedRetries);
        }

        Incident incident =
                runtimeService.createIncidentQuery()
                        .processInstanceId(
                                started.processInstanceId()
                        )
                        .activityId("RefundPayment")
                        .singleResult();

        assertThat(incident).isNotNull();

        assertThat(
                runtimeService.getVariable(
                        started.processInstanceId(),
                        "returnStatus"
                )
        ).isEqualTo("ACCEPTED");
    }

    private ReturnWorkflowResponse moveReturnToAccepted(
            String returnId,
            String orderId,
            String customerId,
            String correlationId
    ) {
        when(orderClient.getOrder(orderId))
                .thenReturn(new OrderClient.OrderDetailsResponse(
                        orderId,
                        customerId,
                        new BigDecimal("120.50"),
                        "USD",
                        "SKU-DEFAULT",
                        1,
                        "COMPLETED",
                        correlationId,
                        null,
                        Instant.now().minusSeconds(24 * 60 * 60)
                ));

        when(returnShippingClient.createReturnShipment(
                any(ReturnShippingClient.CreateReturnShipmentRequest.class),
                eq("return-shipment:" + returnId)
        )).thenReturn(
                new ReturnShippingClient.ReturnShipmentResponse(
                        "shipment-" + returnId,
                        returnId,
                        orderId,
                        customerId,
                        "CREATED",
                        correlationId
                )
        );

        ReturnWorkflowResponse started =
                returnWorkflowService.startReturnWorkflow(
                        new StartReturnWorkflowRequest(
                                returnId,
                                orderId,
                                customerId,
                                "Product is defective",
                                correlationId
                        )
                );

        returnWorkflowService.receiveReturnItem(
                returnId,
                new ReturnItemReceivedRequest(
                        "warehouse-user",
                        Instant.now(),
                        correlationId
                )
        );

        returnWorkflowService.completeInspection(
                returnId,
                new ReturnInspectionRequest(
                        true,
                        "inspector-user",
                        "Return accepted",
                        List.of(
                                new ReturnInspectionItemRequest(
                                        "SKU-DEFAULT",
                                        1,
                                        true
                                )
                        ),
                        correlationId
                )
        );

        return started;
    }

    @Test
void returnExpiresWhenItemIsNotReceivedWithinSevenDays() {
    String returnId = "return-test-1006";
    String orderId = "order-test-1006";
    String customerId = "customer-42";
    String correlationId = "correlation-return-1006";

    when(orderClient.getOrder(orderId))
            .thenReturn(new OrderClient.OrderDetailsResponse(
                    orderId,
                    customerId,
                    new BigDecimal("120.50"),
                    "USD",
                    "SKU-DEFAULT",
                    1,
                    "COMPLETED",
                    correlationId,
                    null,
                    Instant.now().minusSeconds(24 * 60 * 60)
            ));

    when(returnShippingClient.createReturnShipment(
            any(ReturnShippingClient.CreateReturnShipmentRequest.class),
            eq("return-shipment:" + returnId)
    )).thenReturn(
            new ReturnShippingClient.ReturnShipmentResponse(
                    "shipment-" + returnId,
                    returnId,
                    orderId,
                    customerId,
                    "CREATED",
                    correlationId
            )
    );

    ReturnWorkflowResponse started =
            returnWorkflowService.startReturnWorkflow(
                    new StartReturnWorkflowRequest(
                            returnId,
                            orderId,
                            customerId,
                            "Product is defective",
                            correlationId
                    )
            );

    Job timeoutJob =
            managementService.createJobQuery()
                    .processInstanceId(started.processInstanceId())
                    .activityId("ReturnItemTimeout")
                    .singleResult();

    assertThat(timeoutJob).isNotNull();

    managementService.executeJob(timeoutJob.getId());

    assertThat(
            runtimeService.getVariable(
                    started.processInstanceId(),
                    "returnStatus"
            )
    ).isEqualTo("EXPIRED");

    assertThat(
            runtimeService.getVariable(
                    started.processInstanceId(),
                    "failureReason"
            )
    ).isEqualTo(
            "Return item was not received within 7 days"
    );

    assertThat(
            runtimeService.createExecutionQuery()
                    .processInstanceId(started.processInstanceId())
                    .activityId("NotifyReturnExpired")
                    .singleResult()
    ).isNotNull();

    assertThat(
            runtimeService.createExecutionQuery()
                    .processInstanceId(started.processInstanceId())
                    .activityId("WaitForReturnItem")
                    .singleResult()
    ).isNull();
}

@Test
void rejectsSecondReturnForSameOrder() {
    String orderId = "order-test-duplicate";
    String customerId = "customer-42";
    String correlationId = "correlation-return-duplicate";

    when(orderClient.getOrder(orderId))
            .thenReturn(new OrderClient.OrderDetailsResponse(
                    orderId,
                    customerId,
                    new BigDecimal("120.50"),
                    "USD",
                    "SKU-DEFAULT",
                    1,
                    "COMPLETED",
                    correlationId,
                    null,
                    Instant.now().minusSeconds(24 * 60 * 60)
            ));

    when(returnShippingClient.createReturnShipment(
            any(ReturnShippingClient.CreateReturnShipmentRequest.class),
            eq("return-shipment:return-duplicate-1")
    )).thenReturn(
            new ReturnShippingClient.ReturnShipmentResponse(
                    "shipment-duplicate-1",
                    "return-duplicate-1",
                    orderId,
                    customerId,
                    "CREATED",
                    correlationId
            )
    );

    returnWorkflowService.startReturnWorkflow(
            new StartReturnWorkflowRequest(
                    "return-duplicate-1",
                    orderId,
                    customerId,
                    "First return",
                    correlationId
            )
    );

    assertThatThrownBy(
            () -> returnWorkflowService.startReturnWorkflow(
                    new StartReturnWorkflowRequest(
                            "return-duplicate-2",
                            orderId,
                            customerId,
                            "Second return",
                            "correlation-return-duplicate-2"
                    )
            )
    )
            .isInstanceOf(ReturnWorkflowConflictException.class)
            .hasMessageContaining(
                    "Order already has a return request"
            );
}
@Test
void rejectsIneligibleReturnRequests() {
    assertIneligibleOrder(
            "order-not-completed",
            "customer-42",
            "PROCESSING",
            Instant.now().minusSeconds(24 * 60 * 60),
            "customer-42",
            "Order must be COMPLETED"
    );

    assertIneligibleOrder(
            "order-wrong-customer",
            "actual-customer",
            "COMPLETED",
            Instant.now().minusSeconds(24 * 60 * 60),
            "wrong-customer",
            "Customer does not match order"
    );

    assertIneligibleOrder(
            "order-expired-window",
            "customer-42",
            "COMPLETED",
            Instant.now().minusSeconds(16 * 24 * 60 * 60),
            "customer-42",
            "Return window of 15 days has expired"
    );
}

private void assertIneligibleOrder(
        String orderId,
        String actualCustomerId,
        String orderStatus,
        Instant deliveredAt,
        String requestedCustomerId,
        String expectedMessage
) {
    when(orderClient.getOrder(orderId))
            .thenReturn(new OrderClient.OrderDetailsResponse(
                    orderId,
                    actualCustomerId,
                    new BigDecimal("120.50"),
                    "USD",
                    "SKU-DEFAULT",
                    1,
                    orderStatus,
                    "correlation-" + orderId,
                    null,
                    deliveredAt
            ));

    assertThatThrownBy(
            () -> returnWorkflowService.startReturnWorkflow(
                    new StartReturnWorkflowRequest(
                            "return-" + orderId,
                            orderId,
                            requestedCustomerId,
                            "Product is defective",
                            "correlation-" + orderId
                    )
            )
    )
            .isInstanceOf(ReturnWorkflowConflictException.class)
            .hasMessageContaining(expectedMessage);

    assertThat(
            runtimeService.createProcessInstanceQuery()
                    .processInstanceBusinessKey(
                            "return-" + orderId
                    )
                    .count()
    ).isZero();
}
}