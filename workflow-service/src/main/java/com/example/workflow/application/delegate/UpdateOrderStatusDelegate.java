package com.example.workflow.application.delegate;

import com.example.workflow.infrastructure.client.OrderClient;
import com.example.workflow.shared.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UpdateOrderStatusDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateOrderStatusDelegate.class);
    private final OrderClient orderClient;

    public UpdateOrderStatusDelegate(OrderClient orderClient) {
        this.orderClient = orderClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String orderId = (String) execution.getVariable(ProcessVariables.ORDER_ID);
        String statusInput = (String) execution.getVariable("statusInput");

        if (statusInput == null) {
            statusInput = (String) execution.getVariable(ProcessVariables.ORDER_STATUS);
        } else {
            execution.setVariable(ProcessVariables.ORDER_STATUS, statusInput);
        }

        LOGGER.info("[UpdateOrderStatusDelegate] Synchronizing orderId={} status to {}", orderId, statusInput);
        if (orderId != null && statusInput != null) {
            orderClient.updateOrderStatus(orderId, statusInput);
        } else {
            LOGGER.warn("[UpdateOrderStatusDelegate] Cannot update status. orderId={}, status={}", orderId, statusInput);
        }
    }
}
