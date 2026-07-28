package com.example.workflow.application.delegate;

import com.example.workflow.shared.ProcessVariables;
import com.example.workflow.infrastructure.client.InsufficientStockException;
import com.example.workflow.infrastructure.client.InventoryClient;
import com.example.workflow.infrastructure.client.InventoryClient.InventoryReservationRequest;
import com.example.workflow.infrastructure.client.OrderClient;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.ArrayList;
import java.util.List;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ReserveInventoryDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReserveInventoryDelegate.class);
    private static final String INSUFFICIENT_STOCK_ERROR = "INSUFFICIENT_STOCK";
    private final Tracer tracer = GlobalOpenTelemetry.getTracer(ReserveInventoryDelegate.class.getName());

    private final InventoryClient inventoryClient;
    private final OrderClient orderClient;

    public ReserveInventoryDelegate(InventoryClient inventoryClient, OrderClient orderClient) {
        this.inventoryClient = inventoryClient;
        this.orderClient = orderClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Span span = startDelegateSpan("bpmn.reserve_inventory", execution);
        try (Scope ignored = span.makeCurrent()) {
            String businessKey = execution.getBusinessKey();
            String correlationId = stringVariable(execution, ProcessVariables.CORRELATION_ID);
            OrderClient.OrderLine orderLine = currentOrderLine(execution, businessKey);
            span.setAttribute("order.id", businessKey);
            span.setAttribute("inventory.sku", orderLine.sku());
            span.setAttribute("inventory.quantity", orderLine.quantity());

            LOGGER.info("Reserving inventory. businessKey={}, correlationId={}, sku={}, quantity={}",
                    businessKey, correlationId, orderLine.sku(), orderLine.quantity());
            try {
                InventoryClient.InventoryReservationResponse reservation = inventoryClient.reserveInventory(
                        new InventoryReservationRequest(businessKey, orderLine.sku(), orderLine.quantity(), correlationId),
                        inventoryReservationIdempotencyKey(execution, businessKey, orderLine)
                );
                span.setAttribute("inventory.reservation.id", reservation.reservationId());
                span.setAttribute("inventory.status", "RESERVED");
                execution.setVariable(ProcessVariables.INVENTORY_RESERVATION_ID, reservation.reservationId());
                execution.setVariable(ProcessVariables.INVENTORY_RESERVATION_IDS, appendReservationId(execution, reservation.reservationId()));
                execution.setVariable(ProcessVariables.INVENTORY_STATUS, "RESERVED");
            } catch (InsufficientStockException exception) {
                span.recordException(exception);
                span.setStatus(StatusCode.ERROR, exception.getMessage());
                span.setAttribute("inventory.status", "INSUFFICIENT_STOCK");
                execution.setVariable(ProcessVariables.INVENTORY_STATUS, "INSUFFICIENT_STOCK");
                execution.setVariable(ProcessVariables.FAILURE_REASON, exception.getMessage());
                throw new BpmnError(INSUFFICIENT_STOCK_ERROR, exception.getMessage());
            }
        } catch (RuntimeException exception) {
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR, exception.getMessage());
            throw exception;
        } finally {
            span.end();
        }
    }

    private OrderClient.OrderLine currentOrderLine(DelegateExecution execution, String businessKey) {
        Object line = execution.getVariable("orderLine");
        if (line instanceof OrderClient.OrderLine orderLine) {
            return orderLine;
        }
        OrderClient.OrderDetailsResponse order = orderClient.getOrder(businessKey);
        return new OrderClient.OrderLine(order.sku(), order.quantity());
    }

    private String inventoryReservationIdempotencyKey(
            DelegateExecution execution,
            String businessKey,
            OrderClient.OrderLine orderLine
    ) {
        Object nrOfInstances = execution.getVariable("nrOfInstances");
        if (nrOfInstances instanceof Number instances && instances.intValue() > 1) {
            return "inventory-reservation:" + businessKey + ":" + orderLine.sku();
        }
        return "inventory-reservation:" + businessKey;
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

    private Span startDelegateSpan(String spanName, DelegateExecution execution) {
        Span span = tracer.spanBuilder(spanName).startSpan();
        span.setAttribute("camunda.activity.id", execution.getCurrentActivityId());
        span.setAttribute("camunda.business_key", execution.getBusinessKey());
        span.setAttribute("camunda.process_instance.id", execution.getProcessInstanceId());
        span.setAttribute("correlation.id", String.valueOf(execution.getVariable(ProcessVariables.CORRELATION_ID)));
        return span;
    }

    private String stringVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);
        return value == null ? null : String.valueOf(value);
    }
}
