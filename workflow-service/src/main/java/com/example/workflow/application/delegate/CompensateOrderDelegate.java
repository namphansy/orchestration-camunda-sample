package com.example.workflow.application.delegate;

import com.example.workflow.infrastructure.client.InventoryClient;
import com.example.workflow.infrastructure.client.PaymentClient;
import com.example.workflow.shared.ProcessVariables;
import java.util.LinkedHashSet;
import java.util.Set;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CompensateOrderDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompensateOrderDelegate.class);

    private final PaymentClient paymentClient;
    private final InventoryClient inventoryClient;
    private final RuntimeService runtimeService;

    public CompensateOrderDelegate(PaymentClient paymentClient, InventoryClient inventoryClient, RuntimeService runtimeService) {
        this.paymentClient = paymentClient;
        this.inventoryClient = inventoryClient;
        this.runtimeService = runtimeService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String businessKey = execution.getBusinessKey();
        Object paymentStatus = execution.getVariable(ProcessVariables.PAYMENT_STATUS);
        Object paymentTransactionId = execution.getVariable(ProcessVariables.PAYMENT_TRANSACTION_ID);
        if ("CHARGED".equals(paymentStatus) && paymentTransactionId != null) {
            PaymentClient.PaymentChargeResponse refund = paymentClient.refundPayment(
                    String.valueOf(paymentTransactionId),
                    "payment-refund:" + businessKey
            );
            if (refund != null) {
                execution.setVariable(ProcessVariables.PAYMENT_REFUND_STATUS, refund.status());
                execution.setVariable(ProcessVariables.PAYMENT_STATUS, refund.status());
            }
        }

        Set<String> reservationIds = reservationIds(execution);
        LOGGER.info("Compensating inventory reservations. businessKey={}, reservationIds={}", businessKey, reservationIds);
        for (String reservationId : reservationIds) {
            String idempotencyKey = inventoryReleaseIdempotencyKey(execution, businessKey, reservationId, reservationIds.size());
            LOGGER.info("Releasing inventory reservation. businessKey={}, reservationId={}, idempotencyKey={}",
                    businessKey, reservationId, idempotencyKey);
            InventoryClient.InventoryReservationResponse release = inventoryClient.releaseInventory(
                    reservationId,
                    idempotencyKey
            );
            if (release != null) {
                execution.setVariable(ProcessVariables.INVENTORY_RELEASE_STATUS, release.status());
                execution.setVariable(ProcessVariables.INVENTORY_STATUS, release.status());
            }
        }
    }

    private Set<String> reservationIds(DelegateExecution execution) {
        Set<String> reservationIds = new LinkedHashSet<>();
        Object reservationId = runtimeService.getVariable(execution.getProcessInstanceId(), ProcessVariables.INVENTORY_RESERVATION_ID);
        if (reservationId != null) {
            reservationIds.add(String.valueOf(reservationId));
        }

        Object reservationIdList = runtimeService.getVariable(execution.getProcessInstanceId(), ProcessVariables.INVENTORY_RESERVATION_IDS);
        if (reservationIdList instanceof Iterable<?> values) {
            for (Object value : values) {
                if (value != null) {
                    reservationIds.add(String.valueOf(value));
                }
            }
        }
        return reservationIds;
    }

    private String inventoryReleaseIdempotencyKey(
            DelegateExecution execution,
            String businessKey,
            String reservationId,
            int reservationCount
    ) {
        if (reservationCount == 1 && !hasMultipleOrderLines(execution)) {
            return "inventory-release:" + businessKey;
        }
        return "inventory-release:" + businessKey + ":" + reservationId;
    }

    private boolean hasMultipleOrderLines(DelegateExecution execution) {
        Object orderLines = runtimeService.getVariable(execution.getProcessInstanceId(), ProcessVariables.ORDER_LINES);
        return orderLines instanceof Iterable<?> values && values.iterator().hasNext() && hasMoreThanOne(values);
    }

    private boolean hasMoreThanOne(Iterable<?> values) {
        int count = 0;
        for (Object ignored : values) {
            count++;
            if (count > 1) {
                return true;
            }
        }
        return false;
    }
}
