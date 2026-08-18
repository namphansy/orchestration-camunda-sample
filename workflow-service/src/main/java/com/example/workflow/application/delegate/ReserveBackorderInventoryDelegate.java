package com.example.workflow.application.delegate;

import com.example.workflow.infrastructure.client.InventoryClient;
import com.example.workflow.infrastructure.client.InventoryClient.InventoryReservationRequest;
import com.example.workflow.shared.ProcessVariables;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class ReserveBackorderInventoryDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReserveBackorderInventoryDelegate.class);

    private final InventoryClient inventoryClient;

    public ReserveBackorderInventoryDelegate(InventoryClient inventoryClient) {
        this.inventoryClient = inventoryClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String backorderId = execution.getBusinessKey();
        String orderId = stringVariable(execution, ProcessVariables.ORDER_ID);
        if (orderId == null || orderId.isBlank()) {
            orderId = backorderId;
        }
        String correlationId = stringVariable(execution, ProcessVariables.CORRELATION_ID);
        String sku = stringVariable(execution, ProcessVariables.SKU);
        Integer quantity = (Integer) execution.getVariable(ProcessVariables.QUANTITY);

        LOGGER.info("Reserving backorder inventory. backorderId={}, orderId={}, sku={}, quantity={}",
                backorderId, orderId, sku, quantity);

        try {
            InventoryClient.InventoryReservationResponse reservation = inventoryClient.reserveInventory(
                    new InventoryReservationRequest(orderId, sku, quantity, correlationId),
                    "backorder-reservation:" + backorderId + ":" + sku
            );
            execution.setVariable(ProcessVariables.INVENTORY_RESERVATION_ID, reservation.reservationId());
            execution.setVariable(ProcessVariables.INVENTORY_RESERVATION_IDS, appendReservationId(execution, reservation.reservationId()));
            execution.setVariable(ProcessVariables.BACKORDER_STATUS, "RESERVED");
        } catch (Exception exception) {
            LOGGER.error("Failed to reserve backorder inventory. backorderId={}, orderId={}, sku={}, quantity={}",
                    backorderId, orderId, sku, quantity, exception);
            execution.setVariable(ProcessVariables.BACKORDER_STATUS, "FAILED");
            throw exception; // Propagate or handle as needed
        }
    }

    private String stringVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);
        return value == null ? null : String.valueOf(value);
    }

    private List<String> appendReservationId(DelegateExecution execution, String reservationId) {
        Object existing = execution.getVariable(ProcessVariables.INVENTORY_RESERVATION_IDS);
        List<String> reservationIds = new ArrayList<>();
        if (existing instanceof List<?> values) {
            values.stream()
                    .map(String::valueOf)
                    .forEach(reservationIds::add);
        }
        reservationIds.add(reservationId);
        return reservationIds;
    }
}
