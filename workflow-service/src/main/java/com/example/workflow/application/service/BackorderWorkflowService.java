package com.example.workflow.application.service;

import com.example.workflow.api.request.RestockEventRequest;
import com.example.workflow.api.response.BackorderWorkflowResponse;
import com.example.workflow.api.response.RestockEventResponse;
import com.example.workflow.shared.ProcessVariables;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class BackorderWorkflowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackorderWorkflowService.class);
    public static final String RESTOCK_RECEIVED_MESSAGE = "RestockReceived";

    private final RuntimeService runtimeService;
    private final HistoryService historyService;

    public BackorderWorkflowService(RuntimeService runtimeService, HistoryService historyService) {
        this.runtimeService = runtimeService;
        this.historyService = historyService;
    }

    public BackorderWorkflowResponse getBackorderWorkflow(String businessKey) {
        ProcessInstance activeInstance = findActiveBackorder(businessKey);
        if (activeInstance != null) {
            return new BackorderWorkflowResponse(
                    activeInstance.getId(),
                    businessKey,
                    stringRuntimeVariable(activeInstance.getId(), ProcessVariables.BACKORDER_ID),
                    stringRuntimeVariable(activeInstance.getId(), ProcessVariables.ORDER_ID),
                    stringRuntimeVariable(activeInstance.getId(), ProcessVariables.SKU),
                    integerRuntimeVariable(activeInstance.getId(), ProcessVariables.QUANTITY),
                    stringRuntimeVariable(activeInstance.getId(), ProcessVariables.CORRELATION_ID),
                    stringRuntimeVariable(activeInstance.getId(), ProcessVariables.BACKORDER_STATUS),
                    "ACTIVE"
            );
        }

        HistoricProcessInstance historicInstance = findHistoricBackorder(businessKey);
        if (historicInstance == null) {
            throw new BackorderWorkflowNotFoundException(businessKey);
        }

        return new BackorderWorkflowResponse(
                historicInstance.getId(),
                businessKey,
                stringHistoricVariable(historicInstance.getId(), ProcessVariables.BACKORDER_ID),
                stringHistoricVariable(historicInstance.getId(), ProcessVariables.ORDER_ID),
                stringHistoricVariable(historicInstance.getId(), ProcessVariables.SKU),
                integerHistoricVariable(historicInstance.getId(), ProcessVariables.QUANTITY),
                stringHistoricVariable(historicInstance.getId(), ProcessVariables.CORRELATION_ID),
                stringHistoricVariable(historicInstance.getId(), ProcessVariables.BACKORDER_STATUS),
                historicInstance.getEndTime() == null ? "ACTIVE" : "COMPLETED"
        );
    }

    public RestockEventResponse handleRestockEvent(String businessKey, RestockEventRequest request) {
        LOGGER.info("Received restock event. businessKey={}, correlationId={}, sku={}, quantity={}",
                businessKey, request.correlationId(), request.sku(), request.quantity());

        ProcessInstance activeInstance = findActiveBackorder(businessKey);
        if (activeInstance == null) {
            if (findHistoricBackorder(businessKey) != null) {
                return ignored(businessKey, request.correlationId(), "ALREADY_COMPLETED", "Backorder process is already completed");
            }
            return ignored(businessKey, request.correlationId(), "IGNORED", "No active backorder process found");
        }

        String expectedSku = stringRuntimeVariable(activeInstance.getId(), ProcessVariables.SKU);
        Integer expectedQuantity = integerRuntimeVariable(activeInstance.getId(), ProcessVariables.QUANTITY);
        String expectedCorrelationId = stringRuntimeVariable(activeInstance.getId(), ProcessVariables.CORRELATION_ID);

        if (!equalsText(expectedCorrelationId, request.correlationId())) {
            return ignored(businessKey, request.correlationId(), "IGNORED", "Correlation id does not match");
        }
        if (!equalsText(expectedSku, request.sku())) {
            return ignored(businessKey, request.correlationId(), "IGNORED", "SKU does not match");
        }
        if (expectedQuantity != null && request.quantity() < expectedQuantity) {
            return ignored(businessKey, request.correlationId(), "IGNORED", "Restock quantity is lower than required quantity");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put(ProcessVariables.RESTOCK_RECEIVED, true);
        
        try {
            correlateByBackorderIdVariable(businessKey, request, variables);

            LOGGER.info("Restock event correlated. businessKey={}, correlationId={}",
                    businessKey, request.correlationId());
            return new RestockEventResponse(businessKey, request.correlationId(), "CORRELATED", "Restock event correlated");
        } catch (MismatchingMessageCorrelationException exception) {
            try {
                correlateByProcessBusinessKey(businessKey, request, variables);
                LOGGER.info("Restock event correlated by process business key. businessKey={}, correlationId={}",
                        businessKey, request.correlationId());
                return new RestockEventResponse(businessKey, request.correlationId(), "CORRELATED", "Restock event correlated");
            } catch (MismatchingMessageCorrelationException fallbackException) {
                LOGGER.warn("Restock event ignored or already processed. businessKey={}, correlationId={}, reason={}",
                        businessKey, request.correlationId(), fallbackException.getMessage());
                return ignored(businessKey, request.correlationId(), "IGNORED", "Backorder is not waiting for restock");
            }
        }
    }

    private void correlateByBackorderIdVariable(
            String businessKey,
            RestockEventRequest request,
            Map<String, Object> variables
    ) {
        runtimeService.createMessageCorrelation(RESTOCK_RECEIVED_MESSAGE)
                .processInstanceVariableEquals(ProcessVariables.BACKORDER_ID, businessKey)
                .processInstanceVariableEquals(ProcessVariables.CORRELATION_ID, request.correlationId())
                .setVariables(variables)
                .correlateWithResult();
    }

    private void correlateByProcessBusinessKey(
            String businessKey,
            RestockEventRequest request,
            Map<String, Object> variables
    ) {
        runtimeService.createMessageCorrelation(RESTOCK_RECEIVED_MESSAGE)
                .processInstanceBusinessKey(businessKey)
                .processInstanceVariableEquals(ProcessVariables.CORRELATION_ID, request.correlationId())
                .setVariables(variables)
                .correlateWithResult();
    }

    private RestockEventResponse ignored(String businessKey, String correlationId, String status, String reason) {
        LOGGER.info("Restock event not correlated. businessKey={}, correlationId={}, status={}, reason={}",
                businessKey, correlationId, status, reason);
        return new RestockEventResponse(businessKey, correlationId, status, reason);
    }

    private ProcessInstance findActiveBackorder(String businessKey) {
        ProcessInstance byBackorderId = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey("backorder-fulfillment")
                .variableValueEquals(ProcessVariables.BACKORDER_ID, businessKey)
                .singleResult();
        if (byBackorderId != null) {
            return byBackorderId;
        }
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey("backorder-fulfillment")
                .processInstanceBusinessKey(businessKey)
                .singleResult();
    }

    private HistoricProcessInstance findHistoricBackorder(String businessKey) {
        HistoricProcessInstance byBackorderId = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey("backorder-fulfillment")
                .variableValueEquals(ProcessVariables.BACKORDER_ID, businessKey)
                .orderByProcessInstanceStartTime()
                .desc()
                .listPage(0, 1)
                .stream()
                .findFirst()
                .orElse(null);
        if (byBackorderId != null) {
            return byBackorderId;
        }
        return historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey("backorder-fulfillment")
                .processInstanceBusinessKey(businessKey)
                .orderByProcessInstanceStartTime()
                .desc()
                .listPage(0, 1)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private String stringRuntimeVariable(String processInstanceId, String variableName) {
        Object value = runtimeService.getVariable(processInstanceId, variableName);
        return value == null ? null : String.valueOf(value);
    }

    private Integer integerRuntimeVariable(String processInstanceId, String variableName) {
        Object value = runtimeService.getVariable(processInstanceId, variableName);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value == null ? null : Integer.valueOf(String.valueOf(value));
    }

    private String stringHistoricVariable(String processInstanceId, String variableName) {
        HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName(variableName)
                .singleResult();
        return variable == null ? null : String.valueOf(variable.getValue());
    }

    private Integer integerHistoricVariable(String processInstanceId, String variableName) {
        HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName(variableName)
                .singleResult();
        if (variable == null || variable.getValue() == null) {
            return null;
        }
        Object value = variable.getValue();
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private boolean equalsText(String left, String right) {
        return left != null && left.equals(right);
    }
}
