package com.example.shipping.api.request;

import com.example.shipping.domain.model.ShipmentSimulation;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateShipmentRequest(
        @NotBlank String orderId,
        @NotBlank String sku,
        @NotNull @Min(1) Integer quantity,
        String correlationId,
        ShipmentSimulation simulation
) {
    public ShipmentSimulation simulation() {
        return simulation == null ? ShipmentSimulation.NONE : simulation;
    }
}
