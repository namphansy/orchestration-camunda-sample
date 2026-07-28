package com.example.worker.task;

import com.example.worker.config.WorkerProperties;
import jakarta.annotation.PostConstruct;
import org.camunda.bpm.client.ExternalTaskClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "worker", name = "subscriptions-enabled", havingValue = "true", matchIfMissing = true)
public class WorkerSubscriptionRegistrar {

    private final ExternalTaskClient client;
    private final WorkerProperties properties;
    private final ReserveInventoryHandler reserveInventoryHandler;
    private final ChargePaymentHandler chargePaymentHandler;
    private final CreateShipmentHandler createShipmentHandler;

    public WorkerSubscriptionRegistrar(
            ExternalTaskClient client,
            WorkerProperties properties,
            ReserveInventoryHandler reserveInventoryHandler,
            ChargePaymentHandler chargePaymentHandler,
            CreateShipmentHandler createShipmentHandler
    ) {
        this.client = client;
        this.properties = properties;
        this.reserveInventoryHandler = reserveInventoryHandler;
        this.chargePaymentHandler = chargePaymentHandler;
        this.createShipmentHandler = createShipmentHandler;
    }

    @PostConstruct
    void subscribe() {
        client.subscribe(ExternalTaskTopics.RESERVE_INVENTORY)
                .lockDuration(properties.getLockDuration().toMillis())
                .handler(reserveInventoryHandler)
                .open();
        client.subscribe(ExternalTaskTopics.CHARGE_PAYMENT)
                .lockDuration(properties.getLockDuration().toMillis())
                .handler(chargePaymentHandler)
                .open();
        client.subscribe(ExternalTaskTopics.CREATE_SHIPMENT)
                .lockDuration(properties.getLockDuration().toMillis())
                .handler(createShipmentHandler)
                .open();
    }
}
