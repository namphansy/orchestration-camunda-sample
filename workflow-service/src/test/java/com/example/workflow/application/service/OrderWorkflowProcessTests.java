package com.example.workflow.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.workflow.shared.ProcessVariables;
import com.example.workflow.infrastructure.client.InsufficientStockException;
import com.example.workflow.infrastructure.client.InventoryClient;
import com.example.workflow.infrastructure.client.InvoiceClient;
import com.example.workflow.infrastructure.client.NotificationClient;
import com.example.workflow.infrastructure.client.OrderClient;
import com.example.workflow.infrastructure.client.PaymentClient;
import com.example.workflow.infrastructure.client.PaymentDeclinedException;
import java.math.BigDecimal;
import java.util.Map;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.Incident;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = "camunda.bpm.job-execution.enabled=false")
class OrderWorkflowProcessTests {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final ManagementService managementService;
    private final TaskService taskService;

    @MockBean
    private InventoryClient inventoryClient;

    @MockBean
    private OrderClient orderClient;

    @MockBean
    private PaymentClient paymentClient;

    @MockBean
    private InvoiceClient invoiceClient;

    @MockBean
    private NotificationClient notificationClient;

    @Autowired
    OrderWorkflowProcessTests(
            RepositoryService repositoryService,
            RuntimeService runtimeService,
            HistoryService historyService,
            ManagementService managementService,
            TaskService taskService
    ) {
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.managementService = managementService;
        this.taskService = taskService;
    }

    @Test
    void deploysOrderProcessingProcess() {
        long deployedDefinitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(OrderWorkflowService.PROCESS_DEFINITION_KEY)
                .count();

        assertThat(deployedDefinitions).isPositive();
    }

    @Test
    void startsAndCompletesOrderProcessingProcess() {
        String businessKey = "order-process-test-1";
        when(inventoryClient.reserveInventory(any(), eq("inventory-reservation:" + businessKey)))
                .thenReturn(new InventoryClient.InventoryReservationResponse(
                        "reservation-process-test-1",
                        businessKey,
                        "SKU-DEFAULT",
                        1,
                        "RESERVED",
                        "correlation-process-test-1"
                ));
        when(orderClient.getOrder(businessKey))
                .thenReturn(new OrderClient.OrderDetailsResponse(
                        businessKey,
                        "customer-1",
                        new BigDecimal("120.50"),
                        "USD",
                        "SKU-DEFAULT",
                        1,
                        "PROCESSING",
                        "correlation-process-test-1"
                ));
        when(paymentClient.chargePayment(any(), eq("payment-charge:" + businessKey)))
                .thenReturn(new PaymentClient.PaymentChargeResponse(
                        "payment-process-test-1",
                        businessKey,
                        new BigDecimal("120.50"),
                        "USD",
                        "CHARGED",
                        "correlation-process-test-1",
                        null
                ));
        stubInvoiceGenerated(businessKey, "correlation-process-test-1");
        stubNotificationPublished(businessKey, "correlation-process-test-1");

        ProcessInstance processInstance = startOrderProcess(businessKey, "correlation-process-test-1");

        executeSinglePaymentJob(processInstance);
        assertWaitingForPaymentConfirmation(processInstance);
        confirmPayment(processInstance, businessKey, "correlation-process-test-1", "payment-process-test-1", "CHARGED");

        assertThat(runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .singleResult()).isNull();

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .finished()
                .singleResult();
        HistoricVariableInstance orderStatus = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .variableName(ProcessVariables.ORDER_STATUS)
                .singleResult();
        HistoricVariableInstance paymentStatus = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .variableName(ProcessVariables.PAYMENT_STATUS)
                .singleResult();
        HistoricVariableInstance invoiceStatus = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .variableName(ProcessVariables.INVOICE_STATUS)
                .singleResult();
        HistoricVariableInstance notificationStatus = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .variableName(ProcessVariables.NOTIFICATION_STATUS)
                .singleResult();

        assertThat(historicProcessInstance).isNotNull();
        assertThat(orderStatus.getValue()).isEqualTo("COMPLETED");
        assertThat(paymentStatus.getValue()).isEqualTo("CHARGED");
        assertThat(invoiceStatus.getValue()).isEqualTo("GENERATED");
        assertThat(notificationStatus.getValue()).isEqualTo("PUBLISHED");
        verify(invoiceClient).generateInvoice(any(), eq("invoice-generation:" + businessKey));
        verify(notificationClient).publishNotification(any());
    }

    @Test
    void createsManagerApprovalTaskFromDmnAndContinuesWhenApproved() {
        String businessKey = "order-process-test-manager-approval";
        String correlationId = "correlation-process-test-manager-approval";
        stubInventoryReserved(businessKey, correlationId);
        stubOrder(businessKey, new BigDecimal("5000.00"), 1, correlationId);
        stubPaymentCharged(businessKey, new BigDecimal("5000.00"), correlationId);
        stubInvoiceGenerated(businessKey, correlationId);
        stubNotificationPublished(businessKey, correlationId);

        ProcessInstance processInstance = startOrderProcess(businessKey, correlationId);

        executeSinglePaymentJob(processInstance);
        confirmPayment(processInstance, businessKey, correlationId, "payment-" + businessKey, "CHARGED");
        Task task = singleApprovalTask(processInstance, "ManagerApprovalTask");

        assertThat(task.getName()).isEqualTo("Manager Approval");
        assertThat(runtimeService.getVariable(processInstance.getId(), ProcessVariables.APPROVAL_LEVEL))
                .isEqualTo("MANAGER");
        assertThat(taskService.getIdentityLinksForTask(task.getId()))
                .anySatisfy(identityLink -> assertThat(identityLink.getGroupId()).isEqualTo("managers"));

        taskService.complete(task.getId(), Map.of(ProcessVariables.APPROVED, true, ProcessVariables.APPROVER, "manager-1"));

        assertThat(runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .singleResult()).isNull();
        assertHistoricVariable(processInstance, ProcessVariables.ORDER_STATUS, "COMPLETED");
        assertHistoricVariable(processInstance, ProcessVariables.APPROVER, "manager-1");
    }

    @Test
    void createsDirectorApprovalTaskFromDmn() {
        String businessKey = "order-process-test-director-approval";
        String correlationId = "correlation-process-test-director-approval";
        stubInventoryReserved(businessKey, correlationId);
        stubOrder(businessKey, new BigDecimal("50000.00"), 1, correlationId);
        stubPaymentCharged(businessKey, new BigDecimal("50000.00"), correlationId);

        ProcessInstance processInstance = startOrderProcess(businessKey, correlationId);

        executeSinglePaymentJob(processInstance);
        confirmPayment(processInstance, businessKey, correlationId, "payment-" + businessKey, "CHARGED");
        Task task = singleApprovalTask(processInstance, "DirectorApprovalTask");

        assertThat(task.getName()).isEqualTo("Director Approval");
        assertThat(runtimeService.getVariable(processInstance.getId(), ProcessVariables.APPROVAL_LEVEL))
                .isEqualTo("DIRECTOR");
        assertThat(taskService.getIdentityLinksForTask(task.getId()))
                .anySatisfy(identityLink -> assertThat(identityLink.getGroupId()).isEqualTo("directors"));
    }

    @Test
    void rejectedApprovalTriggersOrderCancellationPath() {
        String businessKey = "order-process-test-approval-rejected";
        String correlationId = "correlation-process-test-approval-rejected";
        stubInventoryReserved(businessKey, correlationId);
        stubOrder(businessKey, new BigDecimal("6000.00"), 1, correlationId);
        stubPaymentCharged(businessKey, new BigDecimal("6000.00"), correlationId);

        ProcessInstance processInstance = startOrderProcess(businessKey, correlationId);

        executeSinglePaymentJob(processInstance);
        confirmPayment(processInstance, businessKey, correlationId, "payment-" + businessKey, "CHARGED");
        Task task = singleApprovalTask(processInstance, "ManagerApprovalTask");

        taskService.complete(task.getId(), Map.of(
                ProcessVariables.APPROVED, false,
                ProcessVariables.APPROVER, "manager-2",
                ProcessVariables.FAILURE_REASON, "Rejected by manager"
        ));

        assertThat(runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .singleResult()).isNull();
        assertHistoricVariable(processInstance, ProcessVariables.ORDER_STATUS, "REJECTED");
        assertHistoricVariable(processInstance, ProcessVariables.APPROVED, false);
        assertHistoricVariable(processInstance, ProcessVariables.FAILURE_REASON, "Rejected by manager");
    }

    @Test
    void rejectsOrderWhenInventoryReportsInsufficientStock() {
        String businessKey = "order-process-test-insufficient-stock";
        when(inventoryClient.reserveInventory(any(), eq("inventory-reservation:" + businessKey)))
                .thenThrow(new InsufficientStockException("Inventory service reported insufficient stock"));
        when(orderClient.getOrder(businessKey))
                .thenReturn(new OrderClient.OrderDetailsResponse(
                        businessKey,
                        "customer-1",
                        new BigDecimal("120.50"),
                        "USD",
                        "SKU-DEFAULT",
                        999,
                        "PROCESSING",
                        "correlation-process-test-insufficient-stock"
                ));

        ProcessInstance processInstance = startOrderProcess(businessKey, "correlation-process-test-insufficient-stock");

        HistoricVariableInstance orderStatus = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .variableName(ProcessVariables.ORDER_STATUS)
                .singleResult();
        HistoricVariableInstance inventoryStatus = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .variableName(ProcessVariables.INVENTORY_STATUS)
                .singleResult();

        assertThat(runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .singleResult()).isNull();
        assertThat(orderStatus.getValue()).isEqualTo("REJECTED");
        assertThat(inventoryStatus.getValue()).isEqualTo("INSUFFICIENT_STOCK");
    }

    @Test
    void rejectsOrderWhenPaymentIsDeclined() {
        String businessKey = "order-process-test-payment-declined";
        stubInventoryReserved(businessKey, "correlation-process-test-payment-declined");
        stubOrder(businessKey, new BigDecimal("120.50"), 1, "correlation-process-test-payment-declined");
        when(paymentClient.chargePayment(any(), eq("payment-charge:" + businessKey)))
                .thenThrow(new PaymentDeclinedException("Payment declined by test rule"));

        ProcessInstance processInstance = startOrderProcess(businessKey, "correlation-process-test-payment-declined");

        executeSinglePaymentJob(processInstance);

        HistoricVariableInstance orderStatus = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .variableName(ProcessVariables.ORDER_STATUS)
                .singleResult();
        HistoricVariableInstance paymentStatus = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .variableName(ProcessVariables.PAYMENT_STATUS)
                .singleResult();

        assertThat(runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .singleResult()).isNull();
        assertThat(orderStatus.getValue()).isEqualTo("REJECTED");
        assertThat(paymentStatus.getValue()).isEqualTo("DECLINED");
    }

    @Test
    void technicalPaymentFailureIsRetriedAndCreatesIncidentAfterRetriesAreExhausted() {
        String businessKey = "order-process-test-payment-technical-failure";
        stubInventoryReserved(businessKey, "correlation-process-test-payment-technical-failure");
        stubOrder(businessKey, new BigDecimal("120.50"), 1, "correlation-process-test-payment-technical-failure");
        when(paymentClient.chargePayment(any(), eq("payment-charge:" + businessKey)))
                .thenThrow(new IllegalStateException("Payment processor unavailable"));

        ProcessInstance processInstance = startOrderProcess(businessKey, "correlation-process-test-payment-technical-failure");
        Job paymentJob = singlePaymentJob(processInstance);

        assertThat(paymentJob.getRetries()).isEqualTo(3);
        assertPaymentJobFails(paymentJob);
        paymentJob = singlePaymentJob(processInstance);
        assertThat(paymentJob.getRetries()).isEqualTo(2);

        assertPaymentJobFails(paymentJob);
        paymentJob = singlePaymentJob(processInstance);
        assertThat(paymentJob.getRetries()).isEqualTo(1);

        assertPaymentJobFails(paymentJob);

        Incident incident = runtimeService.createIncidentQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .activityId("ChargePayment")
                .singleResult();

        assertThat(incident).isNotNull();
        assertThat(singlePaymentJob(processInstance).getRetries()).isZero();
    }

    @Test
    void notificationFailureDoesNotCorruptInvoiceProcessing() {
        String businessKey = "order-process-test-notification-failure";
        String correlationId = "correlation-process-test-notification-failure";
        stubInventoryReserved(businessKey, correlationId);
        stubOrder(businessKey, new BigDecimal("120.50"), 1, correlationId);
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
        stubInvoiceGenerated(businessKey, correlationId);
        when(notificationClient.publishNotification(any()))
                .thenThrow(new IllegalStateException("Notification broker unavailable"));

        ProcessInstance processInstance = startOrderProcess(businessKey, correlationId);

        executeSinglePaymentJob(processInstance);
        confirmPayment(processInstance, businessKey, correlationId, "payment-" + businessKey, "CHARGED");

        HistoricVariableInstance orderStatus = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .variableName(ProcessVariables.ORDER_STATUS)
                .singleResult();
        HistoricVariableInstance invoiceStatus = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .variableName(ProcessVariables.INVOICE_STATUS)
                .singleResult();
        HistoricVariableInstance notificationStatus = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .variableName(ProcessVariables.NOTIFICATION_STATUS)
                .singleResult();

        assertThat(runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .singleResult()).isNull();
        assertThat(orderStatus.getValue()).isEqualTo("COMPLETED");
        assertThat(invoiceStatus.getValue()).isEqualTo("GENERATED");
        assertThat(notificationStatus.getValue()).isEqualTo("FAILED");
    }

    @Test
    void paymentConfirmationResumesOnlyMatchingBusinessKeyAndCorrelationId() {
        String businessKey = "order-process-test-payment-confirmation";
        String correlationId = "correlation-process-test-payment-confirmation";
        stubInventoryReserved(businessKey, correlationId);
        stubOrder(businessKey, new BigDecimal("120.50"), 1, correlationId);
        stubPaymentCharged(businessKey, new BigDecimal("120.50"), correlationId);
        stubInvoiceGenerated(businessKey, correlationId);
        stubNotificationPublished(businessKey, correlationId);

        ProcessInstance processInstance = startOrderProcess(businessKey, correlationId);

        executeSinglePaymentJob(processInstance);
        assertWaitingForPaymentConfirmation(processInstance);
        assertThatThrownBy(() -> confirmPayment(processInstance, businessKey, "wrong-correlation", "payment-" + businessKey, "CHARGED"))
                .isInstanceOf(MismatchingMessageCorrelationException.class);

        confirmPayment(processInstance, businessKey, correlationId, "payment-" + businessKey, "CHARGED");

        assertThat(runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .singleResult()).isNull();
        assertHistoricVariable(processInstance, ProcessVariables.PAYMENT_STATUS, "CHARGED");
        assertHistoricVariable(processInstance, ProcessVariables.PAYMENT_CONFIRMED, true);
    }

    @Test
    void duplicatePaymentConfirmationDoesNotAdvanceProcessTwice() {
        String businessKey = "order-process-test-duplicate-payment-confirmation";
        String correlationId = "correlation-process-test-duplicate-payment-confirmation";
        stubInventoryReserved(businessKey, correlationId);
        stubOrder(businessKey, new BigDecimal("5000.00"), 1, correlationId);
        stubPaymentCharged(businessKey, new BigDecimal("5000.00"), correlationId);

        ProcessInstance processInstance = startOrderProcess(businessKey, correlationId);

        executeSinglePaymentJob(processInstance);
        confirmPayment(processInstance, businessKey, correlationId, "payment-" + businessKey, "CHARGED");

        assertThatThrownBy(() -> confirmPayment(processInstance, businessKey, correlationId, "payment-" + businessKey, "CHARGED"))
                .isInstanceOf(MismatchingMessageCorrelationException.class);
        assertThat(taskService.createTaskQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .taskDefinitionKey("ManagerApprovalTask")
                .count()).isEqualTo(1);
    }

    @Test
    void paymentConfirmationTimeoutRejectsOrder() {
        String businessKey = "order-process-test-payment-timeout";
        String correlationId = "correlation-process-test-payment-timeout";
        stubInventoryReserved(businessKey, correlationId);
        stubOrder(businessKey, new BigDecimal("120.50"), 1, correlationId);
        stubPaymentCharged(businessKey, new BigDecimal("120.50"), correlationId);

        ProcessInstance processInstance = startOrderProcess(businessKey, correlationId);

        executeSinglePaymentJob(processInstance);
        Job timeoutJob = singlePaymentConfirmationTimerJob(processInstance);
        managementService.executeJob(timeoutJob.getId());

        assertThat(runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .singleResult()).isNull();
        assertHistoricVariable(processInstance, ProcessVariables.ORDER_STATUS, "REJECTED");
        assertHistoricVariable(processInstance, ProcessVariables.PAYMENT_STATUS, "TIMED_OUT");
        assertHistoricVariable(processInstance, ProcessVariables.FAILURE_REASON, "Payment confirmation timed out");
    }

    private ProcessInstance startOrderProcess(String businessKey, String correlationId) {
        return runtimeService.startProcessInstanceByKey(
                OrderWorkflowService.PROCESS_DEFINITION_KEY,
                businessKey,
                Map.of(
                        ProcessVariables.ORDER_ID, businessKey,
                        ProcessVariables.BUSINESS_KEY, businessKey,
                        ProcessVariables.CORRELATION_ID, correlationId,
                        ProcessVariables.ORDER_STATUS, "CREATED"
                )
        );
    }

    private void stubInventoryReserved(String businessKey, String correlationId) {
        when(inventoryClient.reserveInventory(any(), eq("inventory-reservation:" + businessKey)))
                .thenReturn(new InventoryClient.InventoryReservationResponse(
                        "reservation-" + businessKey,
                        businessKey,
                        "SKU-DEFAULT",
                        1,
                        "RESERVED",
                        correlationId
                ));
    }

    private void stubOrder(String businessKey, BigDecimal amount, Integer quantity, String correlationId) {
        when(orderClient.getOrder(businessKey))
                .thenReturn(new OrderClient.OrderDetailsResponse(
                        businessKey,
                        "customer-1",
                        amount,
                        "USD",
                        "SKU-DEFAULT",
                        quantity,
                        "PROCESSING",
                        correlationId
                ));
    }

    private void stubPaymentCharged(String businessKey, BigDecimal amount, String correlationId) {
        when(paymentClient.chargePayment(any(), eq("payment-charge:" + businessKey)))
                .thenReturn(new PaymentClient.PaymentChargeResponse(
                        "payment-" + businessKey,
                        businessKey,
                        amount,
                        "USD",
                        "CHARGED",
                        correlationId,
                        null
                ));
    }

    private void stubInvoiceGenerated(String businessKey, String correlationId) {
        when(invoiceClient.generateInvoice(any(), eq("invoice-generation:" + businessKey)))
                .thenReturn(new InvoiceClient.InvoiceResponse(
                        "invoice-" + businessKey,
                        businessKey,
                        new BigDecimal("120.50"),
                        "USD",
                        "GENERATED",
                        correlationId
                ));
    }

    private void stubNotificationPublished(String businessKey, String correlationId) {
        when(notificationClient.publishNotification(any()))
                .thenReturn(new NotificationClient.NotificationResponse(
                        "notification-" + businessKey,
                        businessKey,
                        "PUBLISHED",
                        correlationId
                ));
    }

    private void executeSinglePaymentJob(ProcessInstance processInstance) {
        managementService.executeJob(singlePaymentJob(processInstance).getId());
    }

    private void confirmPayment(
            ProcessInstance processInstance,
            String businessKey,
            String correlationId,
            String paymentTransactionId,
            String paymentStatus
    ) {
        runtimeService.createMessageCorrelation(OrderWorkflowService.PAYMENT_CONFIRMATION_MESSAGE)
                .processInstanceBusinessKey(businessKey)
                .processInstanceVariableEquals(ProcessVariables.CORRELATION_ID, correlationId)
                .setVariable(ProcessVariables.PAYMENT_TRANSACTION_ID, paymentTransactionId)
                .setVariable(ProcessVariables.PAYMENT_STATUS, paymentStatus)
                .setVariable(ProcessVariables.PAYMENT_CONFIRMED, true)
                .correlateWithResult();
    }

    private void assertWaitingForPaymentConfirmation(ProcessInstance processInstance) {
        assertThat(runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .activityId("WaitForPaymentConfirmation")
                .singleResult()).isNotNull();
    }

    private void assertPaymentJobFails(Job paymentJob) {
        assertThatThrownBy(() -> managementService.executeJob(paymentJob.getId()))
                .isInstanceOf(RuntimeException.class);
    }

    private Job singlePaymentJob(ProcessInstance processInstance) {
        return managementService.createJobQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .activityId("ChargePayment")
                .singleResult();
    }

    private Job singlePaymentConfirmationTimerJob(ProcessInstance processInstance) {
        return managementService.createJobQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .activityId("PaymentConfirmationTimeoutBoundary")
                .singleResult();
    }

    private Task singleApprovalTask(ProcessInstance processInstance, String taskDefinitionKey) {
        return taskService.createTaskQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .taskDefinitionKey(taskDefinitionKey)
                .singleResult();
    }

    private void assertHistoricVariable(ProcessInstance processInstance, String variableName, Object expectedValue) {
        HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .variableName(variableName)
                .singleResult();
        assertThat(variable.getValue()).isEqualTo(expectedValue);
    }
}
