package com.example.worker.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.worker.client.OrderClient;
import com.example.worker.client.ShippingClient;
import com.example.worker.client.ShippingFailedException;
import com.example.worker.config.WorkerProperties;
import java.math.BigDecimal;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;

class CreateShipmentHandlerTests {

    private final ShippingClient shippingClient = org.mockito.Mockito.mock(ShippingClient.class);
    private final OrderClient orderClient = org.mockito.Mockito.mock(OrderClient.class);
    private final CreateShipmentHandler handler = new CreateShipmentHandler(
            shippingClient,
            orderClient,
            new WorkerFailureHandler(new WorkerProperties())
    );
    private final ExternalTask task = org.mockito.Mockito.mock(ExternalTask.class);
    private final ExternalTaskService service = org.mockito.Mockito.mock(ExternalTaskService.class);

    @Test
    void completesShipmentTask() {
        when(task.getBusinessKey()).thenReturn("order-worker-shipping");
        when(task.getVariable("correlationId")).thenReturn("correlation-worker-shipping");
        when(orderClient.getOrder("order-worker-shipping")).thenReturn(order());
        when(shippingClient.createShipment(any(), eq("shipment-creation:order-worker-shipping")))
                .thenReturn(new ShippingClient.ShipmentResponse(
                        "shipment-worker-shipping",
                        "order-worker-shipping",
                        "SKU-DEFAULT",
                        1,
                        "CREATED",
                        "correlation-worker-shipping",
                        null
                ));

        handler.execute(task, service);

        verify(service).complete(eq(task), org.mockito.ArgumentMatchers.argThat(variables ->
                "shipment-worker-shipping".equals(variables.get("shipmentId"))
                        && "CREATED".equals(variables.get("shipmentStatus"))
        ));
    }

    @Test
    void sendsBpmnErrorForShipmentFailure() {
        when(task.getBusinessKey()).thenReturn("order-worker-shipping-failed");
        when(task.getVariable("correlationId")).thenReturn("correlation-worker-shipping-failed");
        when(orderClient.getOrder("order-worker-shipping-failed")).thenReturn(order());
        when(shippingClient.createShipment(any(), eq("shipment-creation:order-worker-shipping-failed")))
                .thenThrow(new ShippingFailedException("Carrier rejected shipment creation"));

        handler.execute(task, service);

        verify(service).handleBpmnError(
                eq(task),
                eq("SHIPMENT_FAILED"),
                eq("Carrier rejected shipment creation"),
                org.mockito.ArgumentMatchers.argThat(variables -> "FAILED".equals(variables.get("shipmentStatus")))
        );
    }

    private OrderClient.OrderDetailsResponse order() {
        return new OrderClient.OrderDetailsResponse(
                "order-worker-shipping",
                "customer-worker",
                new BigDecimal("120.50"),
                "USD",
                "SKU-DEFAULT",
                1,
                "PROCESSING",
                "correlation-worker-shipping",
                null
        );
    }
}
