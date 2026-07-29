package com.example.worker.task;

import com.example.worker.client.InsufficientStockException;
import com.example.worker.client.InventoryClient;
import com.example.worker.client.OrderClient;
import com.example.worker.shared.ProcessVariables;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ReserveInventoryHandler implements ExternalTaskHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReserveInventoryHandler.class);
    private static final String INSUFFICIENT_STOCK_ERROR = "INSUFFICIENT_STOCK";

    private final InventoryClient inventoryClient;
    private final OrderClient orderClient;
    private final WorkerFailureHandler failureHandler;

    public ReserveInventoryHandler(
            InventoryClient inventoryClient,
            OrderClient orderClient,
            WorkerFailureHandler failureHandler
    ) {
        this.inventoryClient = inventoryClient;
        this.orderClient = orderClient;
        this.failureHandler = failureHandler;
    }

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        String businessKey = businessKey(task);
        String correlationId = stringVariable(task, ProcessVariables.CORRELATION_ID);
        OrderClient.OrderLine orderLine = currentOrderLine(task, businessKey);
        try {
            LOGGER.info("Handling reserve-inventory external task. businessKey={}, correlationId={}, sku={}, quantity={}",
                    businessKey, correlationId, orderLine.sku(), orderLine.quantity());
            InventoryClient.InventoryReservationResponse reservation = inventoryClient.reserveInventory(
                    new InventoryClient.InventoryReservationRequest(
                            businessKey,
                            orderLine.sku(),
                            orderLine.quantity(),
                            correlationId
                    ),
                    inventoryReservationIdempotencyKey(task, businessKey, orderLine)
            );
            Map<String, Object> variables = new HashMap<>();
            variables.put(ProcessVariables.INVENTORY_RESERVATION_ID, reservation.reservationId());
            variables.put(ProcessVariables.INVENTORY_RESERVATION_IDS, appendReservationId(task, reservation.reservationId()));
            variables.put(ProcessVariables.INVENTORY_STATUS, "RESERVED");
            service.complete(task, variables);
        } catch (InsufficientStockException exception) {
            Map<String, Object> variables = Map.of(
                    ProcessVariables.INVENTORY_STATUS, "INSUFFICIENT_STOCK",
                    ProcessVariables.FAILURE_REASON, exception.getMessage()
            );
            service.handleBpmnError(task, INSUFFICIENT_STOCK_ERROR, exception.getMessage(), variables);
        } catch (RuntimeException exception) {
            failureHandler.handleRetryableFailure(task, service, exception);
        }
    }

    private OrderClient.OrderLine currentOrderLine(ExternalTask task, String businessKey) {
        Object line = task.getVariable("orderLine");
        if (line instanceof Map<?, ?> values) {
            return new OrderClient.OrderLine(String.valueOf(values.get("sku")), integerValue(values.get("quantity")));
        }
        OrderClient.OrderDetailsResponse order = orderClient.getOrder(businessKey);
        return order.resolvedOrderLines().get(0);
    }

    private String inventoryReservationIdempotencyKey(ExternalTask task, String businessKey, OrderClient.OrderLine orderLine) {
        Object nrOfInstances = task.getVariable("nrOfInstances");
        if (nrOfInstances instanceof Number instances && instances.intValue() > 1) {
            return "inventory-reservation:" + businessKey + ":" + orderLine.sku();
        }
        return "inventory-reservation:" + businessKey;
    }

    private List<String> appendReservationId(ExternalTask task, String reservationId) {
        Object existing = task.getVariable(ProcessVariables.INVENTORY_RESERVATION_IDS);
        List<String> reservationIds = new ArrayList<>();
        if (existing instanceof List<?> values) {
            values.stream().map(String::valueOf).forEach(reservationIds::add);
        }
        reservationIds.add(reservationId);
        return reservationIds;
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
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
