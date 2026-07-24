package com.example.workflow.application.delegate;

import com.example.workflow.infrastructure.client.OrderClient;
import com.example.workflow.infrastructure.client.PaymentClient;
import com.example.workflow.infrastructure.client.PaymentClient.PaymentChargeRequest;
import com.example.workflow.infrastructure.client.PaymentDeclinedException;
import com.example.workflow.shared.ProcessVariables;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ChargePaymentDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChargePaymentDelegate.class);
    private static final String PAYMENT_DECLINED_ERROR = "PAYMENT_DECLINED";
    private final Tracer tracer = GlobalOpenTelemetry.getTracer(ChargePaymentDelegate.class.getName());

    private final PaymentClient paymentClient;
    private final OrderClient orderClient;

    public ChargePaymentDelegate(PaymentClient paymentClient, OrderClient orderClient) {
        this.paymentClient = paymentClient;
        this.orderClient = orderClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Span span = startDelegateSpan("bpmn.charge_payment", execution);
        try (Scope ignored = span.makeCurrent()) {
            String businessKey = execution.getBusinessKey();
            String correlationId = stringVariable(execution, ProcessVariables.CORRELATION_ID);
            OrderClient.OrderDetailsResponse order = orderClient.getOrder(businessKey);
            span.setAttribute("order.id", businessKey);
            span.setAttribute("payment.amount", order.orderAmount().doubleValue());
            span.setAttribute("payment.currency", order.currency());

            LOGGER.info("Charging payment. businessKey={}, correlationId={}, amount={}, currency={}",
                    businessKey, correlationId, order.orderAmount(), order.currency());
            try {
                PaymentClient.PaymentChargeResponse payment = paymentClient.chargePayment(
                        new PaymentChargeRequest(businessKey, order.orderAmount(), order.currency(), correlationId),
                        "payment-charge:" + businessKey
                );
                span.setAttribute("payment.transaction.id", payment.transactionId());
                span.setAttribute("payment.status", payment.status());
                execution.setVariable(ProcessVariables.PAYMENT_TRANSACTION_ID, payment.transactionId());
                execution.setVariable(ProcessVariables.PAYMENT_STATUS, payment.status());
                execution.setVariable(ProcessVariables.ORDER_AMOUNT, order.orderAmount());
            } catch (PaymentDeclinedException exception) {
                span.recordException(exception);
                span.setStatus(StatusCode.ERROR, exception.getMessage());
                span.setAttribute("payment.status", "DECLINED");
                execution.setVariable(ProcessVariables.PAYMENT_STATUS, "DECLINED");
                execution.setVariable(ProcessVariables.FAILURE_REASON, exception.getMessage());
                throw new BpmnError(PAYMENT_DECLINED_ERROR, exception.getMessage());
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

    private String stringVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);
        return value == null ? null : String.valueOf(value);
    }
}
