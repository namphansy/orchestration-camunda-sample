package com.example.workflow.application.delegate;

import com.example.workflow.infrastructure.client.NotificationClient;
import com.example.workflow.shared.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
public class PublishReturnNotificationDelegate implements JavaDelegate {

    private final NotificationClient notificationClient;

    public PublishReturnNotificationDelegate(
            NotificationClient notificationClient
    ) {
        this.notificationClient = notificationClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String returnId = stringVariable(
                execution,
                ProcessVariables.RETURN_ID
        );

        String returnStatus = stringVariable(
                execution,
                ProcessVariables.RETURN_STATUS
        );

        String message = switch (returnStatus) {
            case "REFUNDED" ->
                    "Return " + returnId
                            + " was refunded successfully.";
            case "REJECTED" ->
                    "Return " + returnId
                            + " was rejected after inspection.";
            case "EXPIRED" ->
                    "Return " + returnId
                            + " expired because the item was not received.";
            default -> throw new IllegalStateException(
                    "Unsupported return status for notification: "
                            + returnStatus
            );
        };

        NotificationClient.NotificationResponse response =
                notificationClient.publishNotification(
                        new NotificationClient.PublishNotificationRequest(
                                stringVariable(
                                        execution,
                                        ProcessVariables.ORDER_ID
                                ),
                                stringVariable(
                                        execution,
                                        ProcessVariables.CUSTOMER_ID
                                ),
                                "EMAIL",
                                message,
                                stringVariable(
                                        execution,
                                        ProcessVariables.CORRELATION_ID
                                )
                        )
                );

        if (response == null
                || !"PUBLISHED".equals(response.status())) {
            throw new IllegalStateException(
                    "Return notification was not published"
            );
        }

        execution.setVariable(
                ProcessVariables.NOTIFICATION_ID,
                response.notificationId()
        );

        execution.setVariable(
                ProcessVariables.NOTIFICATION_STATUS,
                response.status()
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
