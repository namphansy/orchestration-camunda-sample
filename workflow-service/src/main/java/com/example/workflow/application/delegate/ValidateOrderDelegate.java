package com.example.workflow.application.delegate;

import com.example.workflow.infrastructure.client.OrderClient;
import com.example.workflow.shared.ProcessVariables;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.List;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ValidateOrderDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateOrderDelegate.class);
    private final Tracer tracer = GlobalOpenTelemetry.getTracer(ValidateOrderDelegate.class.getName());
    private final OrderClient orderClient;

    public ValidateOrderDelegate(OrderClient orderClient) {
        this.orderClient = orderClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Span span = startDelegateSpan("bpmn.validate_order", execution);
        try (Scope ignored = span.makeCurrent()) {
            OrderClient.OrderDetailsResponse order = orderClient.getOrder(execution.getBusinessKey());
            List<OrderClient.OrderLine> orderLines = order.resolvedOrderLines();
            LOGGER.info("Validated order in learning workflow. businessKey={}, correlationId={}, lineCount={}",
                    execution.getBusinessKey(),
                    execution.getVariable(ProcessVariables.CORRELATION_ID),
                    orderLines.size());
            execution.setVariable(ProcessVariables.ORDER_STATUS, "VALIDATED");
            execution.setVariable(ProcessVariables.ORDER_AMOUNT, order.orderAmount());
            execution.setVariable(ProcessVariables.ORDER_LINES, orderLines);
            span.setAttribute("order.status", "VALIDATED");
            span.setAttribute("order.line_count", orderLines.size());
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
