package com.example.workflow.application.delegate;

import com.example.workflow.application.service.ReturnWorkflowConflictException;
import com.example.workflow.infrastructure.client.OrderClient;
import com.example.workflow.shared.ProcessVariables;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
public class ValidateReturnEligibilityDelegate implements JavaDelegate {

    private final OrderClient orderClient;
    private final Clock clock = Clock.systemUTC();

    public ValidateReturnEligibilityDelegate(
            OrderClient orderClient
    ) {
        this.orderClient = orderClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String orderId = stringVariable(
                execution,
                ProcessVariables.ORDER_ID
        );

        String customerId = stringVariable(
                execution,
                ProcessVariables.CUSTOMER_ID
        );

        OrderClient.OrderDetailsResponse order =
                orderClient.getOrder(orderId);

        if (!"COMPLETED".equals(order.status())) {
            throw new ReturnWorkflowConflictException(
                    "Order must be COMPLETED before return: " + orderId
            );
        }

        if (!customerId.equals(order.customerId())) {
            throw new ReturnWorkflowConflictException(
                    "Customer does not match order: " + orderId
            );
        }

        if (order.deliveredAt() == null) {
            throw new ReturnWorkflowConflictException(
                    "Order delivery time is missing: " + orderId
            );
        }

        Instant deadline = order.deliveredAt()
                .plus(15, ChronoUnit.DAYS);

        if (Instant.now(clock).isAfter(deadline)) {
            throw new ReturnWorkflowConflictException(
                    "Return window of 15 days has expired for order: "
                            + orderId
            );
        }

        execution.setVariable(
                ProcessVariables.ORDER_LINES,
                order.resolvedOrderLines()
        );

        execution.setVariable(
                ProcessVariables.ORDER_AMOUNT,
                order.orderAmount()
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
