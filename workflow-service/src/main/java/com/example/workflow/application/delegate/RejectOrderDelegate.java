package com.example.workflow.application.delegate;

import com.example.workflow.infrastructure.client.OrderClient;
import com.example.workflow.shared.ProcessVariables;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RejectOrderDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(RejectOrderDelegate.class);
    private final Tracer tracer = GlobalOpenTelemetry.getTracer(RejectOrderDelegate.class.getName());
    private final OrderClient orderClient;

    public RejectOrderDelegate(OrderClient orderClient) {
        this.orderClient = orderClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Span span = startDelegateSpan("bpmn.reject_order", execution);
        try (Scope ignored = span.makeCurrent()) {
            String orderId = (String) execution.getVariable(ProcessVariables.ORDER_ID);
            execution.setVariable(ProcessVariables.ORDER_STATUS, "REJECTED");
            Object failureReason = execution.getVariable(ProcessVariables.FAILURE_REASON);
            span.setAttribute("order.status", "REJECTED");
            span.setAttribute("failure.reason", String.valueOf(failureReason));
            LOGGER.info("Rejected order. businessKey={}, correlationId={}, reason={}",
                    execution.getBusinessKey(),
                    execution.getVariable(ProcessVariables.CORRELATION_ID),
                    failureReason);
            if (orderId != null) {
                orderClient.updateOrderStatus(orderId, "REJECTED");
            }
        } catch (RuntimeException exception) {
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR, exception.getMessage());
            throw exception;
        } finally {
            span.end();
        }
    }

    private Span startDelegateSpan(String spanName, DelegateExecution execution) {
        Span span = tracer.spanBuilder(spanName).startSpan();
        span.setAttribute("camunda.activity.id", execution.getCurrentActivityId());
        span.setAttribute("camunda.business_key", execution.getBusinessKey());
        span.setAttribute("camunda.process_instance.id", execution.getProcessInstanceId());
        span.setAttribute("correlation.id", String.valueOf(execution.getVariable(ProcessVariables.CORRELATION_ID)));
        return span;
    }
}
