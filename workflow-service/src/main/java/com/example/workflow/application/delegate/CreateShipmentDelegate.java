package com.example.workflow.application.delegate;

import com.example.workflow.infrastructure.client.OrderClient;
import com.example.workflow.infrastructure.client.ShippingClient;
import com.example.workflow.infrastructure.client.ShippingFailedException;
import com.example.workflow.shared.ProcessVariables;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
public class CreateShipmentDelegate implements JavaDelegate {

    private final ShippingClient shippingClient;
    private final OrderClient orderClient;

    public CreateShipmentDelegate(ShippingClient shippingClient, OrderClient orderClient) {
        this.shippingClient = shippingClient;
        this.orderClient = orderClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String businessKey = businessKey(execution);
        String correlationId = String.valueOf(execution.getVariable(ProcessVariables.CORRELATION_ID));
        OrderClient.OrderDetailsResponse order = orderClient.getOrder(businessKey);
        try {
            ShippingClient.ShipmentResponse shipment = shippingClient.createShipment(
                    new ShippingClient.CreateShipmentRequest(
                            businessKey,
                            order.sku(),
                            order.quantity(),
                            correlationId
                    ),
                    "shipment-creation:" + businessKey
            );
            execution.setVariable(ProcessVariables.SHIPMENT_ID, shipment.shipmentId());
            execution.setVariable(ProcessVariables.SHIPMENT_STATUS, shipment.status());
        } catch (ShippingFailedException exception) {
            execution.setVariable(ProcessVariables.SHIPMENT_STATUS, "FAILED");
            execution.setVariable(ProcessVariables.FAILURE_REASON, exception.getMessage());
            throw new BpmnError("SHIPMENT_FAILED", exception.getMessage());
        }
    }

    private String businessKey(DelegateExecution execution) {
        String businessKey = execution.getBusinessKey();
        if (businessKey != null && !businessKey.isBlank()) {
            return businessKey;
        }
        Object value = execution.getVariable(ProcessVariables.BUSINESS_KEY);
        return value == null ? null : String.valueOf(value);
    }
}
