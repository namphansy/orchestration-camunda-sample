package com.example.workflow.application.delegate;

import com.example.workflow.infrastructure.client.NotificationClient;
import com.example.workflow.infrastructure.client.NotificationClient.PublishNotificationRequest;
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
public class PublishNotificationDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublishNotificationDelegate.class);
    private final Tracer tracer = GlobalOpenTelemetry.getTracer(PublishNotificationDelegate.class.getName());

    private final NotificationClient notificationClient;
    private final OrderClient orderClient;

    public PublishNotificationDelegate(NotificationClient notificationClient, OrderClient orderClient) {
        this.notificationClient = notificationClient;
        this.orderClient = orderClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Span span = startDelegateSpan("bpmn.publish_notification", execution);
        try (Scope ignored = span.makeCurrent()) {
            String businessKey = execution.getBusinessKey();
            String correlationId = stringVariable(execution, ProcessVariables.CORRELATION_ID);
            OrderClient.OrderDetailsResponse order = orderClient.getOrder(businessKey);

            LOGGER.info("Publishing notification. businessKey={}, correlationId={}, customerId={}",
                    businessKey, correlationId, order.customerId());
            try {
                NotificationClient.NotificationResponse notification = notificationClient.publishNotification(
                        new PublishNotificationRequest(
                                businessKey,
                                order.customerId(),
                                "EMAIL",
                                "Order " + businessKey + " has been paid.",
                                correlationId
                        )
                );
                span.setAttribute("notification.id", notification.notificationId());
                span.setAttribute("notification.status", notification.status());
                execution.setVariable(ProcessVariables.NOTIFICATION_ID, notification.notificationId());
                execution.setVariable(ProcessVariables.NOTIFICATION_STATUS, notification.status());
            } catch (RuntimeException exception) {
                LOGGER.warn("Notification publishing failed; workflow will continue. businessKey={}, correlationId={}",
                        businessKey, correlationId, exception);
                span.recordException(exception);
                span.setStatus(StatusCode.ERROR, exception.getMessage());
                execution.setVariable(ProcessVariables.NOTIFICATION_STATUS, "FAILED");
            }
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
