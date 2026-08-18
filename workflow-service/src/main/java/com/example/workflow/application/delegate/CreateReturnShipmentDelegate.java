package com.example.workflow.application.delegate;

import com.example.workflow.infrastructure.client.ReturnShippingClient;
import com.example.workflow.shared.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
public class CreateReturnShipmentDelegate implements JavaDelegate {

    private final ReturnShippingClient returnShippingClient;

    public CreateReturnShipmentDelegate(
            ReturnShippingClient returnShippingClient
    ) {
        this.returnShippingClient = returnShippingClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String returnId = stringVariable(
                execution,
                ProcessVariables.RETURN_ID
        );

        ReturnShippingClient.ReturnShipmentResponse shipment =
                returnShippingClient.createReturnShipment(
                        new ReturnShippingClient
                                .CreateReturnShipmentRequest(
                                returnId,
                                stringVariable(
                                        execution,
                                        ProcessVariables.ORDER_ID
                                ),
                                stringVariable(
                                        execution,
                                        ProcessVariables.CUSTOMER_ID
                                ),
                                stringVariable(
                                        execution,
                                        ProcessVariables.CORRELATION_ID
                                )
                        ),
                        "return-shipment:" + returnId
                );

        if (shipment == null) {
            throw new IllegalStateException(
                    "Return shipment service returned no response"
            );
        }

        execution.setVariable(
                ProcessVariables.RETURN_SHIPMENT_ID,
                shipment.returnShipmentId()
        );

        execution.setVariable(
                ProcessVariables.RETURN_STATUS,
                "WAITING_FOR_ITEM"
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
