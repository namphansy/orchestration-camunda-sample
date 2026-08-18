package com.example.workflow.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.execute;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.historyService;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.job;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.runtimeService;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.workflow.api.request.RestockEventRequest;
import com.example.workflow.api.response.BackorderWorkflowResponse;
import com.example.workflow.api.response.RestockEventResponse;
import com.example.workflow.application.delegate.MarkBackorderExpiredDelegate;
import com.example.workflow.application.delegate.MarkBackorderReservedDelegate;
import com.example.workflow.application.delegate.MarkBackorderWaitingForRestockDelegate;
import com.example.workflow.application.delegate.ReserveBackorderInventoryDelegate;
import com.example.workflow.infrastructure.client.InventoryClient;
import com.example.workflow.shared.ProcessVariables;
import java.util.HashMap;
import java.util.Map;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.bpm.spring.boot.starter.test.helper.AbstractProcessEngineRuleTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class BackorderWorkflowProcessTests extends AbstractProcessEngineRuleTest {

    private InventoryClient inventoryClient;
    private BackorderWorkflowService backorderWorkflowService;

    @Before
    public void setUp() {
        inventoryClient = Mockito.mock(InventoryClient.class);
        backorderWorkflowService = new BackorderWorkflowService(runtimeService(), historyService());
        
        Mocks.register("markBackorderWaitingForRestockDelegate", new MarkBackorderWaitingForRestockDelegate());
        Mocks.register("reserveBackorderInventoryDelegate", new ReserveBackorderInventoryDelegate(inventoryClient));
        Mocks.register("markBackorderReservedDelegate", new MarkBackorderReservedDelegate());
        Mocks.register("markBackorderExpiredDelegate", new MarkBackorderExpiredDelegate());
    }

    @Test
    @Deployment(resources = "processes/backorder-fulfillment.bpmn")
    public void testBackorderRestockSuccess() {
        Map<String, Object> variables = new HashMap<>();
        variables.put(ProcessVariables.BUSINESS_KEY, "order-1001");
        variables.put(ProcessVariables.SKU, "SKU-DEFAULT");
        variables.put(ProcessVariables.QUANTITY, 2);
        variables.put(ProcessVariables.CORRELATION_ID, "corr-1001");

        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
                "backorder-fulfillment", "order-1001", variables);

        assertThat(processInstance).isWaitingAt("ReceiveRestockEvent");
        assertThat(processInstance).hasVariables(ProcessVariables.BACKORDER_STATUS);
        assertThat(runtimeService().getVariable(processInstance.getId(), ProcessVariables.BACKORDER_STATUS))
                .isEqualTo("WAITING_FOR_RESTOCK");

        Mockito.when(inventoryClient.reserveInventory(Mockito.any(), Mockito.any()))
                .thenReturn(new InventoryClient.InventoryReservationResponse("res-1001", "order-1001", "SKU-DEFAULT", 2, "RESERVED", "corr-1001"));

        runtimeService().createMessageCorrelation("RestockReceived")
                .processInstanceBusinessKey("order-1001")
                .processInstanceVariableEquals(ProcessVariables.CORRELATION_ID, "corr-1001")
                .setVariable(ProcessVariables.RESTOCK_RECEIVED, true)
                .correlate();

        assertThat(processInstance).isEnded();
        assertThat(processInstance).hasPassed("MarkBackorderReserved", "BackorderCompleted");
    }

    @Test
    @Deployment(resources = "processes/backorder-fulfillment.bpmn")
    public void testQueryBackorderStatus() {
        ProcessInstance processInstance = startBackorder("backorder-query-1", "order-query-1", "SKU-DEFAULT", 2, "corr-query-1");

        BackorderWorkflowResponse response = backorderWorkflowService.getBackorderWorkflow("backorder-query-1");

        assertThat(response.processInstanceId()).isEqualTo(processInstance.getId());
        assertThat(response.backorderId()).isEqualTo("backorder-query-1");
        assertThat(response.orderId()).isEqualTo("order-query-1");
        assertThat(response.sku()).isEqualTo("SKU-DEFAULT");
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.correlationId()).isEqualTo("corr-query-1");
        assertThat(response.backorderStatus()).isEqualTo("WAITING_FOR_RESTOCK");
        assertThat(response.workflowStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @Deployment(resources = "processes/backorder-fulfillment.bpmn")
    public void testWrongCorrelationIsIgnored() {
        ProcessInstance processInstance = startBackorder("backorder-wrong-correlation", "order-wrong-correlation",
                "SKU-DEFAULT", 2, "corr-expected");

        RestockEventResponse response = backorderWorkflowService.handleRestockEvent(
                "backorder-wrong-correlation",
                new RestockEventRequest("SKU-DEFAULT", 5, "corr-wrong")
        );

        assertThat(response.status()).isEqualTo("IGNORED");
        assertThat(response.reason()).isEqualTo("Correlation id does not match");
        assertThat(processInstance).isWaitingAt("ReceiveRestockEvent");
        verify(inventoryClient, never()).reserveInventory(Mockito.any(), Mockito.any());
    }

    @Test
    @Deployment(resources = "processes/backorder-fulfillment.bpmn")
    public void testWrongSkuIsIgnored() {
        ProcessInstance processInstance = startBackorder("backorder-wrong-sku", "order-wrong-sku",
                "SKU-DEFAULT", 2, "corr-sku");

        RestockEventResponse response = backorderWorkflowService.handleRestockEvent(
                "backorder-wrong-sku",
                new RestockEventRequest("SKU-OTHER", 5, "corr-sku")
        );

        assertThat(response.status()).isEqualTo("IGNORED");
        assertThat(response.reason()).isEqualTo("SKU does not match");
        assertThat(processInstance).isWaitingAt("ReceiveRestockEvent");
        verify(inventoryClient, never()).reserveInventory(Mockito.any(), Mockito.any());
    }

    @Test
    @Deployment(resources = "processes/backorder-fulfillment.bpmn")
    public void testInsufficientRestockQuantityIsIgnored() {
        ProcessInstance processInstance = startBackorder("backorder-low-quantity", "order-low-quantity",
                "SKU-DEFAULT", 10, "corr-quantity");

        RestockEventResponse response = backorderWorkflowService.handleRestockEvent(
                "backorder-low-quantity",
                new RestockEventRequest("SKU-DEFAULT", 9, "corr-quantity")
        );

        assertThat(response.status()).isEqualTo("IGNORED");
        assertThat(response.reason()).isEqualTo("Restock quantity is lower than required quantity");
        assertThat(processInstance).isWaitingAt("ReceiveRestockEvent");
        verify(inventoryClient, never()).reserveInventory(Mockito.any(), Mockito.any());
    }

    @Test
    @Deployment(resources = "processes/backorder-fulfillment.bpmn")
    public void testDuplicateRestockEventDoesNotReserveTwice() {
        ProcessInstance processInstance = startBackorder("backorder-duplicate", "order-duplicate",
                "SKU-DEFAULT", 2, "corr-duplicate");
        Mockito.when(inventoryClient.reserveInventory(Mockito.any(), Mockito.any()))
                .thenReturn(new InventoryClient.InventoryReservationResponse(
                        "res-duplicate",
                        "order-duplicate",
                        "SKU-DEFAULT",
                        2,
                        "RESERVED",
                        "corr-duplicate"
                ));

        RestockEventResponse firstResponse = backorderWorkflowService.handleRestockEvent(
                "backorder-duplicate",
                new RestockEventRequest("SKU-DEFAULT", 2, "corr-duplicate")
        );
        RestockEventResponse duplicateResponse = backorderWorkflowService.handleRestockEvent(
                "backorder-duplicate",
                new RestockEventRequest("SKU-DEFAULT", 2, "corr-duplicate")
        );

        assertThat(firstResponse.status()).isEqualTo("CORRELATED");
        assertThat(duplicateResponse.status()).isEqualTo("ALREADY_COMPLETED");
        assertThat(processInstance).isEnded();
        verify(inventoryClient, times(1)).reserveInventory(Mockito.any(), Mockito.any());
    }

    @Test
    @Deployment(resources = "processes/backorder-fulfillment.bpmn")
    public void testBackorderTimeout() {
        Map<String, Object> variables = new HashMap<>();
        variables.put(ProcessVariables.BUSINESS_KEY, "order-timeout-1");
        variables.put(ProcessVariables.SKU, "SKU-DEFAULT");
        variables.put(ProcessVariables.QUANTITY, 2);
        variables.put(ProcessVariables.CORRELATION_ID, "corr-1001");

        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
                "backorder-fulfillment", "order-timeout-1", variables);

        assertThat(processInstance).isWaitingAt("ReceiveRestockEvent");

        // Trigger the timer boundary event
        execute(job(processInstance));

        assertThat(processInstance).isEnded();
        assertThat(processInstance).hasPassed("MarkBackorderExpired", "BackorderExpired");
    }

    private ProcessInstance startBackorder(
            String backorderId,
            String orderId,
            String sku,
            Integer quantity,
            String correlationId
    ) {
        Map<String, Object> variables = new HashMap<>();
        variables.put(ProcessVariables.BUSINESS_KEY, backorderId);
        variables.put(ProcessVariables.BACKORDER_ID, backorderId);
        variables.put(ProcessVariables.ORDER_ID, orderId);
        variables.put(ProcessVariables.SKU, sku);
        variables.put(ProcessVariables.QUANTITY, quantity);
        variables.put(ProcessVariables.CORRELATION_ID, correlationId);

        return runtimeService().startProcessInstanceByKey("backorder-fulfillment", backorderId, variables);
    }
}
