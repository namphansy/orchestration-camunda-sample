package com.example.workflow.application.delegate;

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
public class CompleteOrderDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompleteOrderDelegate.class);
    private final Tracer tracer = GlobalOpenTelemetry.getTracer(CompleteOrderDelegate.class.getName());

    @Override
    public void execute(DelegateExecution execution) {
        Span span = startDelegateSpan("bpmn.complete_order", execution);
        try (Scope ignored = span.makeCurrent()) {
            LOGGER.info("Completed order in learning workflow. businessKey={}, correlationId={}",
                    execution.getBusinessKey(),
                    execution.getVariable(ProcessVariables.CORRELATION_ID));
            execution.setVariable(ProcessVariables.ORDER_STATUS, "COMPLETED");
            span.setAttribute("order.status", "COMPLETED");
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
