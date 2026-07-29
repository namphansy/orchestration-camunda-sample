package com.example.worker.task;

import com.example.worker.client.OrderClient;
import com.example.worker.client.PaymentClient;
import com.example.worker.client.PaymentDeclinedException;
import com.example.worker.shared.ProcessVariables;
import java.util.HashMap;
import java.util.Map;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ChargePaymentHandler implements ExternalTaskHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChargePaymentHandler.class);
    private static final String PAYMENT_DECLINED_ERROR = "PAYMENT_DECLINED";

    private final PaymentClient paymentClient;
    private final OrderClient orderClient;
    private final WorkerFailureHandler failureHandler;

    public ChargePaymentHandler(PaymentClient paymentClient, OrderClient orderClient, WorkerFailureHandler failureHandler) {
        this.paymentClient = paymentClient;
        this.orderClient = orderClient;
        this.failureHandler = failureHandler;
    }

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        String businessKey = businessKey(task);
        String correlationId = stringVariable(task, ProcessVariables.CORRELATION_ID);
        try {
            OrderClient.OrderDetailsResponse order = orderClient.getOrder(businessKey);
            LOGGER.info("Handling charge-payment external task. businessKey={}, correlationId={}, amount={}, currency={}",
                    businessKey, correlationId, order.orderAmount(), order.currency());
            PaymentClient.PaymentChargeResponse payment = paymentClient.chargePayment(
                    new PaymentClient.PaymentChargeRequest(
                            businessKey,
                            order.orderAmount(),
                            order.currency(),
                            correlationId
                    ),
                    "payment-charge:" + businessKey
            );
            Map<String, Object> variables = new HashMap<>();
            variables.put(ProcessVariables.PAYMENT_TRANSACTION_ID, payment.transactionId());
            variables.put(ProcessVariables.PAYMENT_STATUS, "PENDING");
            variables.put(ProcessVariables.PAYMENT_CONFIRMED, false);
            variables.put(ProcessVariables.ORDER_AMOUNT, order.orderAmount());
            service.complete(task, variables);
        } catch (PaymentDeclinedException exception) {
            Map<String, Object> variables = Map.of(
                    ProcessVariables.PAYMENT_STATUS, "DECLINED",
                    ProcessVariables.FAILURE_REASON, exception.getMessage()
            );
            service.handleBpmnError(task, PAYMENT_DECLINED_ERROR, exception.getMessage(), variables);
        } catch (RuntimeException exception) {
            failureHandler.handleRetryableFailure(task, service, exception);
        }
    }

    private String businessKey(ExternalTask task) {
        String businessKey = task.getBusinessKey();
        if (businessKey != null && !businessKey.isBlank()) {
            return businessKey;
        }
        return stringVariable(task, ProcessVariables.BUSINESS_KEY);
    }

    private String stringVariable(ExternalTask task, String variableName) {
        Object value = task.getVariable(variableName);
        return value == null ? null : String.valueOf(value);
    }
}
