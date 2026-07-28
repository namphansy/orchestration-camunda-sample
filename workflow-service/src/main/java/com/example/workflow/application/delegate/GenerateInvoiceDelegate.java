package com.example.workflow.application.delegate;

import com.example.workflow.infrastructure.client.InvoiceClient;
import com.example.workflow.infrastructure.client.InvoiceClient.GenerateInvoiceRequest;
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
public class GenerateInvoiceDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateInvoiceDelegate.class);
    private final Tracer tracer = GlobalOpenTelemetry.getTracer(GenerateInvoiceDelegate.class.getName());

    private final InvoiceClient invoiceClient;
    private final OrderClient orderClient;

    public GenerateInvoiceDelegate(InvoiceClient invoiceClient, OrderClient orderClient) {
        this.invoiceClient = invoiceClient;
        this.orderClient = orderClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Span span = startDelegateSpan("bpmn.generate_invoice", execution);
        try (Scope ignored = span.makeCurrent()) {
            String businessKey = businessKey(execution);
            String correlationId = stringVariable(execution, ProcessVariables.CORRELATION_ID);
            OrderClient.OrderDetailsResponse order = orderClient.getOrder(businessKey);

            LOGGER.info("Generating invoice. businessKey={}, correlationId={}, amount={}, currency={}",
                    businessKey, correlationId, order.orderAmount(), order.currency());
            InvoiceClient.InvoiceResponse invoice = invoiceClient.generateInvoice(
                    new GenerateInvoiceRequest(businessKey, order.orderAmount(), order.currency(), correlationId),
                    "invoice-generation:" + businessKey
            );
            span.setAttribute("invoice.id", invoice.invoiceId());
            span.setAttribute("invoice.status", invoice.status());
            execution.setVariable(ProcessVariables.INVOICE_ID, invoice.invoiceId());
            execution.setVariable(ProcessVariables.INVOICE_STATUS, invoice.status());
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
        span.setAttribute("camunda.business_key", businessKey(execution));
        span.setAttribute("camunda.process_instance.id", execution.getProcessInstanceId());
        span.setAttribute("correlation.id", String.valueOf(execution.getVariable(ProcessVariables.CORRELATION_ID)));
        return span;
    }

    private String stringVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);
        return value == null ? null : String.valueOf(value);
    }

    private String businessKey(DelegateExecution execution) {
        String businessKey = execution.getBusinessKey();
        if (businessKey != null && !businessKey.isBlank()) {
            return businessKey;
        }
        return stringVariable(execution, ProcessVariables.BUSINESS_KEY);
    }
}
