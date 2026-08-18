package com.example.workflow.application.delegate;

import com.example.workflow.api.request.ReturnInspectionItemRequest;
import com.example.workflow.infrastructure.client.InventoryClient;
import com.example.workflow.shared.ProcessVariables;
import java.util.List;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
public class RestockReturnedInventoryDelegate implements JavaDelegate {

    private final InventoryClient inventoryClient;

    public RestockReturnedInventoryDelegate(
            InventoryClient inventoryClient
    ) {
        this.inventoryClient = inventoryClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String returnId = stringVariable(
                execution,
                ProcessVariables.RETURN_ID
        );

        String orderId = stringVariable(
                execution,
                ProcessVariables.ORDER_ID
        );

        String correlationId = stringVariable(
                execution,
                ProcessVariables.CORRELATION_ID
        );

        @SuppressWarnings("unchecked")
        List<ReturnInspectionItemRequest> inspectionItems =
                (List<ReturnInspectionItemRequest>)
                        execution.getVariable(
                                ProcessVariables.INSPECTION_ITEMS
                        );
 
        if (inspectionItems == null) {
            throw new IllegalStateException(
                    "Inspection items are missing"
            );
        }

        boolean restockedAnyItem = false;

        for (ReturnInspectionItemRequest item : inspectionItems) {
            if (!Boolean.TRUE.equals(item.restockable())) {
                continue;
            }

            InventoryClient.InventoryRestockResponse response =
                    inventoryClient.restockInventory(
                            new InventoryClient
                                    .InventoryRestockRequest(
                                    returnId,
                                    orderId,
                                    item.sku(),
                                    item.quantity(),
                                    correlationId
                            ),
                            "inventory-restock:"
                                    + returnId
                                    + ":"
                                    + item.sku()
                    );

            if (response == null
                    || !"RESTOCKED".equals(response.status())) {
                throw new IllegalStateException(
                        "Inventory restock did not complete for SKU: "
                                + item.sku()
                );
            }

            restockedAnyItem = true;
        }

        execution.setVariable(
                ProcessVariables.RESTOCK_STATUS,
                restockedAnyItem
                        ? "RESTOCKED"
                        : "NOT_REQUIRED"
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
