package com.example.worker.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.worker.client.InsufficientStockException;
import com.example.worker.client.InventoryClient;
import com.example.worker.client.OrderClient;
import com.example.worker.config.WorkerProperties;
import java.util.Map;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;

class ReserveInventoryHandlerTests {

    private final InventoryClient inventoryClient = org.mockito.Mockito.mock(InventoryClient.class);
    private final OrderClient orderClient = org.mockito.Mockito.mock(OrderClient.class);
    private final ReserveInventoryHandler handler = new ReserveInventoryHandler(
            inventoryClient,
            orderClient,
            new WorkerFailureHandler(new WorkerProperties())
    );
    private final ExternalTask task = org.mockito.Mockito.mock(ExternalTask.class);
    private final ExternalTaskService service = org.mockito.Mockito.mock(ExternalTaskService.class);

    @Test
    void completesInventoryReservationTask() {
        when(task.getBusinessKey()).thenReturn("order-worker-inventory");
        when(task.getVariable("correlationId")).thenReturn("correlation-worker-inventory");
        when(task.getVariable("orderLine")).thenReturn(Map.of("sku", "SKU-A", "quantity", 2));
        when(task.getVariable("nrOfInstances")).thenReturn(2);
        when(inventoryClient.reserveInventory(any(), eq("inventory-reservation:order-worker-inventory:SKU-A")))
                .thenReturn(new InventoryClient.InventoryReservationResponse(
                        "reservation-worker-inventory",
                        "order-worker-inventory",
                        "SKU-A",
                        2,
                        "RESERVED",
                        "correlation-worker-inventory"
                ));

        handler.execute(task, service);

        verify(service).complete(eq(task), org.mockito.ArgumentMatchers.argThat(variables ->
                "RESERVED".equals(variables.get("inventoryStatus"))
                        && "reservation-worker-inventory".equals(variables.get("inventoryReservationId"))
        ));
    }

    @Test
    void sendsBpmnErrorForInsufficientStock() {
        when(task.getBusinessKey()).thenReturn("order-worker-stock");
        when(task.getVariable("correlationId")).thenReturn("correlation-worker-stock");
        when(task.getVariable("orderLine")).thenReturn(Map.of("sku", "SKU-A", "quantity", 2));
        when(inventoryClient.reserveInventory(any(), eq("inventory-reservation:order-worker-stock")))
                .thenThrow(new InsufficientStockException("Inventory service reported insufficient stock"));

        handler.execute(task, service);

        verify(service).handleBpmnError(
                eq(task),
                eq("INSUFFICIENT_STOCK"),
                eq("Inventory service reported insufficient stock"),
                org.mockito.ArgumentMatchers.argThat(variables -> "INSUFFICIENT_STOCK".equals(variables.get("inventoryStatus")))
        );
    }
}
