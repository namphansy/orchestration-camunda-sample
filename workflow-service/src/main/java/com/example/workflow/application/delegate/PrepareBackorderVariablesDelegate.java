package com.example.workflow.application.delegate;

import com.example.workflow.infrastructure.client.OrderClient;
import com.example.workflow.shared.ProcessVariables;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PrepareBackorderVariablesDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareBackorderVariablesDelegate.class);

    private final RuntimeService runtimeService;

    public PrepareBackorderVariablesDelegate(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String orderId = stringVariable(execution, ProcessVariables.ORDER_ID);
        if (orderId == null || orderId.isBlank()) {
            orderId = execution.getBusinessKey();
        }

        OrderClient.OrderLine orderLine = currentOrderLine(execution);
        String backorderId = "backorder:" + orderId + ":" + orderLine.sku();

        setProcessVariable(execution, ProcessVariables.ORDER_ID, orderId);
        setProcessVariable(execution, ProcessVariables.BACKORDER_ID, backorderId);
        setProcessVariable(execution, ProcessVariables.SKU, orderLine.sku());
        setProcessVariable(execution, ProcessVariables.QUANTITY, orderLine.quantity());
        setProcessVariable(execution, ProcessVariables.BACKORDER_STATUS, "WAITING_FOR_RESTOCK");

        LOGGER.info("Prepared backorder variables. orderId={}, backorderId={}, sku={}, quantity={}",
                orderId, backorderId, orderLine.sku(), orderLine.quantity());
    }

    private OrderClient.OrderLine currentOrderLine(DelegateExecution execution) {
        Object line = execution.getVariable("orderLine");
        if (line instanceof OrderClient.OrderLine orderLine) {
            return orderLine;
        }
        String sku = stringVariable(execution, ProcessVariables.SKU);
        Object quantity = execution.getVariable(ProcessVariables.QUANTITY);
        if (sku == null || quantity == null) {
            throw new IllegalStateException("Cannot prepare backorder without sku and quantity");
        }
        return new OrderClient.OrderLine(sku, ((Number) quantity).intValue());
    }

    private String stringVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);
        return value == null ? null : String.valueOf(value);
    }

    private void setProcessVariable(DelegateExecution execution, String variableName, Object value) {
        runtimeService.setVariable(execution.getProcessInstanceId(), variableName, value);
    }
}
