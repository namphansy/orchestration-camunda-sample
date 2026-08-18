package com.example.workflow.application.service;

import com.example.workflow.api.request.StartReturnWorkflowRequest;
import com.example.workflow.api.response.ReturnWorkflowResponse;
import com.example.workflow.infrastructure.client.OrderClient;
import com.example.workflow.shared.ProcessVariables;
import com.example.workflow.api.request.ReturnInspectionItemRequest;
import com.example.workflow.api.request.ReturnInspectionRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import com.example.workflow.api.request.ReturnItemReceivedRequest;
import com.example.workflow.api.response.ReturnEventResponse;
import org.camunda.bpm.engine.MismatchingMessageCorrelationException;

@Service
public class ReturnWorkflowService {

    public static final String PROCESS_DEFINITION_KEY =
            "return-refund";

    public static final String ITEM_RECEIVED_MESSAGE =
            "ReturnItemReceived";

    public static final String INSPECTION_COMPLETED_MESSAGE =
            "ReturnInspectionCompleted";

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final OrderClient orderClient;
    private final Clock clock;

    public ReturnWorkflowService(
            RuntimeService runtimeService,
            HistoryService historyService,
            OrderClient orderClient
    ) {
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.orderClient = orderClient;
        this.clock = Clock.systemUTC();
    }

    public ReturnWorkflowResponse startReturnWorkflow(StartReturnWorkflowRequest request) {
        validateNoExistingReturn(request);

        OrderClient.OrderDetailsResponse order =
                validateEligibility(request);

        Map<String, Object> variables = new HashMap<>();

        variables.put(
                ProcessVariables.RETURN_ID,
                request.returnId()
        );

        variables.put(
                ProcessVariables.ORDER_ID,
                request.orderId()
        );

        variables.put(
                ProcessVariables.BUSINESS_KEY,
                request.returnId()
        );

        variables.put(
                ProcessVariables.CUSTOMER_ID,
                request.customerId()
        );

        variables.put(
                ProcessVariables.RETURN_REASON,
                request.returnReason()
        );

        variables.put(
                ProcessVariables.CORRELATION_ID,
                request.correlationId()
        );

        variables.put(
                ProcessVariables.RETURN_STATUS,
                "REQUESTED"
        );

        variables.put(
                ProcessVariables.ORDER_LINES,
                order.resolvedOrderLines()
        );

        ProcessInstance processInstance =
                runtimeService.startProcessInstanceByKey(
                        PROCESS_DEFINITION_KEY,
                        request.returnId(),
                        variables
                );

        String returnStatus = String.valueOf(
                runtimeService.getVariable(
                        processInstance.getId(),
                        ProcessVariables.RETURN_STATUS
                )
        );

        return new ReturnWorkflowResponse(
                processInstance.getId(),
                request.returnId(),
                request.orderId(),
                request.correlationId(),
                returnStatus,
                true,
                null
        );
    }

    public ReturnEventResponse receiveReturnItem(
            String returnId,
            ReturnItemReceivedRequest request
    ) {
        ReturnWorkflowResponse workflow =
                getReturnWorkflow(returnId);

        if (!workflow.active()) {
            return ignored(returnId, request.correlationId());
        }

        if (!request.correlationId().equals(
                workflow.correlationId()
        )) {
            return ignored(returnId, request.correlationId());
        }

        Map<String, Object> variables = new HashMap<>();

        variables.put(
                ProcessVariables.RECEIVED_BY,
                request.receivedBy()
        );

        variables.put(
                ProcessVariables.RECEIVED_AT,
                request.receivedAt().toString()
        );

        try {
            runtimeService
                    .createMessageCorrelation(
                            ITEM_RECEIVED_MESSAGE
                    )
                    .processInstanceBusinessKey(returnId)
                    .processInstanceVariableEquals(
                            ProcessVariables.CORRELATION_ID,
                            request.correlationId()
                    )
                    .setVariables(variables)
                    .correlateWithResult();

            return new ReturnEventResponse(
                    returnId,
                    request.correlationId(),
                    "CORRELATED"
            );
        } catch (MismatchingMessageCorrelationException exception) {
            return ignored(returnId, request.correlationId());
        }
    }

    public ReturnEventResponse completeInspection(
            String returnId,
            ReturnInspectionRequest request
    ) {
        ReturnWorkflowResponse workflow =
                getReturnWorkflow(returnId);

        if (!workflow.active()) {
            return ignored(returnId, request.correlationId());
        }

        if (!request.correlationId().equals(
                workflow.correlationId()
        )) {
            return ignored(returnId, request.correlationId());
        }

        validateInspectionItems(
                workflow.orderId(),
                request.items()
        );

        Map<String, Object> variables = new HashMap<>();

        variables.put(
                ProcessVariables.INSPECTION_ACCEPTED,
                request.inspectionAccepted()
        );

        variables.put(
                ProcessVariables.INSPECTOR,
                request.inspector()
        );

        variables.put(
                ProcessVariables.INSPECTION_COMMENT,
                request.comment()
        );

        variables.put(
                ProcessVariables.INSPECTION_ITEMS,
                request.items()
        );

        try {
            runtimeService
                    .createMessageCorrelation(
                            INSPECTION_COMPLETED_MESSAGE
                    )
                    .processInstanceBusinessKey(returnId)
                    .processInstanceVariableEquals(
                            ProcessVariables.CORRELATION_ID,
                            request.correlationId()
                    )
                    .setVariables(variables)
                    .correlateWithResult();

            return new ReturnEventResponse(
                    returnId,
                    request.correlationId(),
                    "CORRELATED"
            );
        } catch (MismatchingMessageCorrelationException exception) {
            return ignored(returnId, request.correlationId());
        }
    }

    private OrderClient.OrderDetailsResponse validateEligibility(StartReturnWorkflowRequest request) {
        OrderClient.OrderDetailsResponse order =
                orderClient.getOrder(request.orderId());

        if (!"COMPLETED".equals(order.status())) {
            throw new ReturnWorkflowConflictException(
                    "Order must be COMPLETED before return: "
                            + request.orderId()
            );
        }

        if (!request.customerId().equals(order.customerId())) {
            throw new ReturnWorkflowConflictException(
                    "Customer does not match order: "
                            + request.orderId()
            );
        }

        if (order.deliveredAt() == null) {
            throw new ReturnWorkflowConflictException(
                    "Order delivery time is missing: "
                            + request.orderId()
            );
        }

        Instant returnDeadline = order.deliveredAt()
                .plus(15, ChronoUnit.DAYS);

        if (Instant.now(clock).isAfter(returnDeadline)) {
            throw new ReturnWorkflowConflictException(
                    "Return window of 15 days has expired for order: "
                            + request.orderId()
            );
        }

        return order;
    }

    public ReturnWorkflowResponse getReturnWorkflow(String returnId) {
        ProcessInstance activeInstance =
                runtimeService.createProcessInstanceQuery()
                        .processDefinitionKey(PROCESS_DEFINITION_KEY)
                        .processInstanceBusinessKey(returnId)
                        .singleResult();

        if (activeInstance != null) {
            return new ReturnWorkflowResponse(
                    activeInstance.getId(),
                    returnId,
                    stringRuntimeVariable(
                            activeInstance.getId(),
                            ProcessVariables.ORDER_ID
                    ),
                    stringRuntimeVariable(
                            activeInstance.getId(),
                            ProcessVariables.CORRELATION_ID
                    ),
                    stringRuntimeVariable(
                            activeInstance.getId(),
                            ProcessVariables.RETURN_STATUS
                    ),
                    true,
                    stringRuntimeVariable(
                            activeInstance.getId(),
                            ProcessVariables.FAILURE_REASON
                    )
            );
        }

        HistoricProcessInstance historicInstance =
                historyService.createHistoricProcessInstanceQuery()
                        .processDefinitionKey(PROCESS_DEFINITION_KEY)
                        .processInstanceBusinessKey(returnId)
                        .orderByProcessInstanceStartTime()
                        .desc()
                        .listPage(0, 1)
                        .stream()
                        .findFirst()
                        .orElseThrow(() ->
                                new ReturnWorkflowNotFoundException(returnId)
                        );

        return new ReturnWorkflowResponse(
                historicInstance.getId(),
                returnId,
                historicVariable(
                        historicInstance.getId(),
                        ProcessVariables.ORDER_ID
                ),
                historicVariable(
                        historicInstance.getId(),
                        ProcessVariables.CORRELATION_ID
                ),
                historicVariable(
                        historicInstance.getId(),
                        ProcessVariables.RETURN_STATUS
                ),
                false,
                historicVariable(
                        historicInstance.getId(),
                        ProcessVariables.FAILURE_REASON
                )
        );
    }

    private void validateNoExistingReturn(StartReturnWorkflowRequest request) {
        boolean returnIdExists =
                runtimeService.createProcessInstanceQuery()
                        .processDefinitionKey(PROCESS_DEFINITION_KEY)
                        .processInstanceBusinessKey(request.returnId())
                        .count() > 0
                || historyService
                        .createHistoricProcessInstanceQuery()
                        .processDefinitionKey(PROCESS_DEFINITION_KEY)
                        .processInstanceBusinessKey(request.returnId())
                        .count() > 0;

        if (returnIdExists) {
            throw new ReturnWorkflowConflictException(
                    "Return workflow already exists: "
                            + request.returnId()
            );
        }

        boolean orderAlreadyHasReturn =
                runtimeService.createProcessInstanceQuery()
                        .processDefinitionKey(PROCESS_DEFINITION_KEY)
                        .variableValueEquals(
                                ProcessVariables.ORDER_ID,
                                request.orderId()
                        )
                        .count() > 0
                || historyService
                        .createHistoricProcessInstanceQuery()
                        .processDefinitionKey(PROCESS_DEFINITION_KEY)
                        .variableValueEquals(
                                ProcessVariables.ORDER_ID,
                                request.orderId()
                        )
                        .count() > 0;

        if (orderAlreadyHasReturn) {
            throw new ReturnWorkflowConflictException(
                    "Order already has a return request: "
                            + request.orderId()
            );
        }
    }

    private String stringRuntimeVariable(
            String processInstanceId,
            String variableName
    ) {
        Object value = runtimeService.getVariable(
                processInstanceId,
                variableName
        );

        return value == null ? null : String.valueOf(value);
    }

    private String historicVariable(
            String processInstanceId,
            String variableName
    ) {
        HistoricVariableInstance variable =
                historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .variableName(variableName)
                        .singleResult();

        return variable == null
                ? null
                : String.valueOf(variable.getValue());
    }

    private ReturnEventResponse ignored(
            String returnId,
            String correlationId
    ) {
        return new ReturnEventResponse(
                returnId,
                correlationId,
                "IGNORED"
        );
    }

    private void validateInspectionItems(
            String orderId,
            List<ReturnInspectionItemRequest> items
    ) {
        OrderClient.OrderDetailsResponse order =
                orderClient.getOrder(orderId);

        Set<String> seenSkus = new HashSet<>();

        for (ReturnInspectionItemRequest item : items) {
            if (!seenSkus.add(item.sku())) {
                throw new ReturnWorkflowConflictException(
                        "Duplicate inspection SKU: " + item.sku()
                );
            }

            OrderClient.OrderLine matchingLine =
                    order.resolvedOrderLines()
                            .stream()
                            .filter(line ->
                                    line.sku().equals(item.sku())
                            )
                            .findFirst()
                            .orElseThrow(() ->
                                    new ReturnWorkflowConflictException(
                                            "Inspection SKU does not belong "
                                                    + "to order: "
                                                    + item.sku()
                                    )
                            );

            if (item.quantity() > matchingLine.quantity()) {
                throw new ReturnWorkflowConflictException(
                        "Inspection quantity exceeds ordered quantity "
                                + "for SKU: "
                                + item.sku()
                );
            }
        }
    }
}