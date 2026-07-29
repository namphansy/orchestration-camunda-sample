package com.example.worker.task;

import com.example.worker.client.OrderClient;
import com.example.worker.client.ShippingClient;
import com.example.worker.client.ShippingFailedException;
import com.example.worker.shared.ProcessVariables;
import java.util.Map;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CreateShipmentHandler implements ExternalTaskHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateShipmentHandler.class);
    private static final String SHIPMENT_FAILED_ERROR = "SHIPMENT_FAILED";

    private final ShippingClient shippingClient;
    private final OrderClient orderClient;
    private final WorkerFailureHandler failureHandler;

    public CreateShipmentHandler(ShippingClient shippingClient, OrderClient orderClient, WorkerFailureHandler failureHandler) {
        this.shippingClient = shippingClient;
        this.orderClient = orderClient;
        this.failureHandler = failureHandler;
    }

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        String businessKey = businessKey(task);
        String correlationId = stringVariable(task, ProcessVariables.CORRELATION_ID);
        try {
            OrderClient.OrderDetailsResponse order = orderClient.getOrder(businessKey);
            LOGGER.info("Handling create-shipment external task. businessKey={}, correlationId={}",
                    businessKey, correlationId);
            ShippingClient.ShipmentResponse shipment = shippingClient.createShipment(
                    new ShippingClient.CreateShipmentRequest(
                            businessKey,
                            order.sku(),
                            order.quantity(),
                            correlationId
                    ),
                    "shipment-creation:" + businessKey
            );
            service.complete(task, Map.of(
                    ProcessVariables.SHIPMENT_ID, shipment.shipmentId(),
                    ProcessVariables.SHIPMENT_STATUS, shipment.status()
            ));
        } catch (ShippingFailedException exception) {
            Map<String, Object> variables = Map.of(
                    ProcessVariables.SHIPMENT_STATUS, "FAILED",
                    ProcessVariables.FAILURE_REASON, exception.getMessage()
            );
            service.handleBpmnError(task, SHIPMENT_FAILED_ERROR, exception.getMessage(), variables);
        } catch (RuntimeException exception) {
            failureHandler.handleRetryableFailure(task, service, exception);
        }
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
