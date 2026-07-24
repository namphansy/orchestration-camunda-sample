package com.example.workflow.application.service;

import com.example.workflow.api.request.StartOrderWorkflowRequest;
import com.example.workflow.api.response.OrderWorkflowResponse;
import com.example.workflow.shared.ProcessVariables;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

@Service
public class OrderWorkflowService {

    public static final String PROCESS_DEFINITION_KEY = "order-processing";

    private final Tracer tracer = GlobalOpenTelemetry.getTracer(OrderWorkflowService.class.getName());

    private final RuntimeService runtimeService;
    private final HistoryService historyService;

    public OrderWorkflowService(RuntimeService runtimeService, HistoryService historyService) {
        this.runtimeService = runtimeService;
        this.historyService = historyService;
    }

    public OrderWorkflowResponse startOrderWorkflow(StartOrderWorkflowRequest request) {
        Span span = tracer.spanBuilder("workflow.start").startSpan();
        try (Scope ignored = span.makeCurrent()) {
            String businessKey = request.orderId();
            String correlationId = normalizeCorrelationId(request.correlationId());
            span.setAttribute("order.id", request.orderId());
            span.setAttribute("correlation.id", correlationId);
            span.setAttribute("camunda.process_definition.key", PROCESS_DEFINITION_KEY);
            span.setAttribute("camunda.business_key", businessKey);

            Map<String, Object> variables = new HashMap<>();
            variables.put(ProcessVariables.ORDER_ID, request.orderId());
            variables.put(ProcessVariables.BUSINESS_KEY, businessKey);
            variables.put(ProcessVariables.CORRELATION_ID, correlationId);
            variables.put(ProcessVariables.ORDER_STATUS, "CREATED");

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    PROCESS_DEFINITION_KEY,
                    businessKey,
                    variables
            );

            span.setAttribute("camunda.process_instance.id", processInstance.getProcessInstanceId());
            String status = isActive(processInstance.getProcessInstanceId()) ? "ACTIVE" : "COMPLETED";
            span.setAttribute("workflow.status", status);
            return new OrderWorkflowResponse(processInstance.getProcessInstanceId(), businessKey, correlationId, status);
        } catch (RuntimeException exception) {
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR, exception.getMessage());
            throw exception;
        } finally {
            span.end();
        }
    }

    public OrderWorkflowResponse getOrderWorkflow(String businessKey) {
        ProcessInstance activeInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .singleResult();
        if (activeInstance != null) {
            Object correlationId = runtimeService.getVariable(activeInstance.getId(), ProcessVariables.CORRELATION_ID);
            return new OrderWorkflowResponse(activeInstance.getId(), businessKey, String.valueOf(correlationId), "ACTIVE");
        }

        HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .orderByProcessInstanceStartTime()
                .desc()
                .listPage(0, 1)
                .stream()
                .findFirst()
                .orElseThrow(() -> new OrderWorkflowNotFoundException(businessKey));

        return new OrderWorkflowResponse(
                historicInstance.getId(),
                businessKey,
                getHistoricVariable(historicInstance.getId(), ProcessVariables.CORRELATION_ID),
                historicInstance.getEndTime() == null ? "ACTIVE" : "COMPLETED"
        );
    }

    private boolean isActive(String processInstanceId) {
        return runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult() != null;
    }

    private String normalizeCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private String getHistoricVariable(String processInstanceId, String variableName) {
        HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName(variableName)
                .singleResult();
        return variable == null ? null : String.valueOf(variable.getValue());
    }
}
