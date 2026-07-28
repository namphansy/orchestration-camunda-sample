package com.example.workflow.application.delegate;

import com.example.workflow.infrastructure.client.InventoryClient;
import com.example.workflow.infrastructure.client.PaymentClient;
import com.example.workflow.shared.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
public class CompensateOrderDelegate implements JavaDelegate {

    private final PaymentClient paymentClient;
    private final InventoryClient inventoryClient;

    public CompensateOrderDelegate(PaymentClient paymentClient, InventoryClient inventoryClient) {
        this.paymentClient = paymentClient;
        this.inventoryClient = inventoryClient;
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

        Object reservationId = execution.getVariable(ProcessVariables.INVENTORY_RESERVATION_ID);
        if (reservationId != null) {
            InventoryClient.InventoryReservationResponse release = inventoryClient.releaseInventory(
                    String.valueOf(reservationId),
                    "inventory-release:" + businessKey
            );
            if (release != null) {
                execution.setVariable(ProcessVariables.INVENTORY_RELEASE_STATUS, release.status());
                execution.setVariable(ProcessVariables.INVENTORY_STATUS, release.status());
            }
        }
    }
}
