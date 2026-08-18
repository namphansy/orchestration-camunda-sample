package com.example.workflow.application.delegate;

import com.example.workflow.infrastructure.client.PaymentClient;
import com.example.workflow.shared.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
public class RefundReturnPaymentDelegate implements JavaDelegate {

    private final PaymentClient paymentClient;

    public RefundReturnPaymentDelegate(
            PaymentClient paymentClient
    ) {
        this.paymentClient = paymentClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String orderId = stringVariable(
                execution,
                ProcessVariables.ORDER_ID
        );

        String returnId = stringVariable(
                execution,
                ProcessVariables.RETURN_ID
        );

        String paymentTransactionId = stringVariable(
                execution,
                ProcessVariables.PAYMENT_TRANSACTION_ID
        );

        if (paymentTransactionId == null) {
            PaymentClient.PaymentChargeResponse chargedPayment =
                    paymentClient
                            .getChargedTransactionByOrderId(orderId);

            if (chargedPayment == null) {
                throw new IllegalStateException(
                        "Charged payment was not found for order: "
                                + orderId
                );
            }

            paymentTransactionId =
                    chargedPayment.transactionId();

            execution.setVariable(
                    ProcessVariables.PAYMENT_TRANSACTION_ID,
                    paymentTransactionId
            );
        }

        PaymentClient.PaymentChargeResponse refund =
                paymentClient.refundPayment(
                        paymentTransactionId,
                        "return-refund:" + returnId
                );

        if (refund == null
                || !"REFUNDED".equals(refund.status())) {
            throw new IllegalStateException(
                    "Payment refund did not complete for order: "
                            + orderId
            );
        }

        execution.setVariable(
                ProcessVariables.REFUND_TRANSACTION_ID,
                refund.transactionId()
        );
    }

    private String stringVariable(
            DelegateExecution execution,
            String variableName
    ) {
        Object value = execution.getVariable(variableName);
        return value == null ? null : String.valueOf(value);
    }
}
