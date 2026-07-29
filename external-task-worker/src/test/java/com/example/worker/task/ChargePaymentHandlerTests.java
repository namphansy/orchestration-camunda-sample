package com.example.worker.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.worker.client.OrderClient;
import com.example.worker.client.PaymentClient;
import com.example.worker.client.PaymentDeclinedException;
import com.example.worker.config.WorkerProperties;
import java.math.BigDecimal;
import java.util.Map;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChargePaymentHandlerTests {

    private final PaymentClient paymentClient = org.mockito.Mockito.mock(PaymentClient.class);
    private final OrderClient orderClient = org.mockito.Mockito.mock(OrderClient.class);
    private final WorkerFailureHandler failureHandler = new WorkerFailureHandler(new WorkerProperties());
    private final ChargePaymentHandler handler = new ChargePaymentHandler(paymentClient, orderClient, failureHandler);
    private final ExternalTask task = org.mockito.Mockito.mock(ExternalTask.class);
    private final ExternalTaskService service = org.mockito.Mockito.mock(ExternalTaskService.class);

    @Test
    void completesChargePaymentTask() {
        when(task.getBusinessKey()).thenReturn("order-worker-payment");
        when(task.getVariable("correlationId")).thenReturn("correlation-worker-payment");
        when(orderClient.getOrder("order-worker-payment")).thenReturn(order());
        when(paymentClient.chargePayment(any(), eq("payment-charge:order-worker-payment")))
                .thenReturn(new PaymentClient.PaymentChargeResponse(
                        "payment-worker-payment",
                        "order-worker-payment",
                        new BigDecimal("120.50"),
                        "USD",
                        "CHARGED",
                        "correlation-worker-payment",
                        null
                ));

        handler.execute(task, service);

        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(service).complete(eq(task), variables.capture());
        org.assertj.core.api.Assertions.assertThat(variables.getValue())
                .containsEntry("paymentTransactionId", "payment-worker-payment")
                .containsEntry("paymentStatus", "PENDING")
                .containsEntry("paymentConfirmed", false);
    }

    @Test
    void sendsBpmnErrorForPaymentDecline() {
        when(task.getBusinessKey()).thenReturn("order-worker-declined");
        when(task.getVariable("correlationId")).thenReturn("correlation-worker-declined");
        when(orderClient.getOrder("order-worker-declined")).thenReturn(order());
        when(paymentClient.chargePayment(any(), eq("payment-charge:order-worker-declined")))
                .thenThrow(new PaymentDeclinedException("Payment declined by test rule"));

        handler.execute(task, service);

        verify(service).handleBpmnError(
                eq(task),
                eq("PAYMENT_DECLINED"),
                eq("Payment declined by test rule"),
                org.mockito.ArgumentMatchers.argThat(variables -> "DECLINED".equals(variables.get("paymentStatus")))
        );
    }

    @Test
    void reportsRetryableFailureForTechnicalError() {
        when(task.getBusinessKey()).thenReturn("order-worker-technical");
        when(task.getVariable("correlationId")).thenReturn("correlation-worker-technical");
        when(task.getRetries()).thenReturn(3);
        when(orderClient.getOrder("order-worker-technical")).thenThrow(new IllegalStateException("order-service unavailable"));

        handler.execute(task, service);

        verify(service).handleFailure(eq(task), eq("order-service unavailable"), contains("order-service unavailable"), eq(2), eq(30_000L));
    }

    private OrderClient.OrderDetailsResponse order() {
        return new OrderClient.OrderDetailsResponse(
                "order-worker-payment",
                "customer-worker",
                new BigDecimal("120.50"),
                "USD",
                "SKU-DEFAULT",
                1,
                "PROCESSING",
                "correlation-worker-payment",
                null
        );
    }
}
