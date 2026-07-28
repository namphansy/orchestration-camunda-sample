package com.example.shipping.api.controller;

import com.example.shipping.api.request.CreateShipmentRequest;
import com.example.shipping.api.response.ShipmentResponse;
import com.example.shipping.application.service.ShippingService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipments")
public class ShippingController {

    private final ShippingService shippingService;

    public ShippingController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentResponse createShipment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateShipmentRequest request
    ) {
        return shippingService.createShipment(request, idempotencyKey);
    }

    @PostMapping("/{shipmentId}/cancel")
    public ShipmentResponse cancelShipment(
            @PathVariable("shipmentId") String shipmentId,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return shippingService.cancelShipment(shipmentId, idempotencyKey);
    }

    @GetMapping("/{shipmentId}")
    public ShipmentResponse getShipment(@PathVariable("shipmentId") String shipmentId) {
        return shippingService.getShipment(shipmentId);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, Object> handleTechnicalFailure(IllegalStateException exception) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "service", "shipping-service",
                "errorCode", "SHIPPING_TECHNICAL_FAILURE",
                "message", exception.getMessage(),
                "details", Map.of()
        );
    }
}
