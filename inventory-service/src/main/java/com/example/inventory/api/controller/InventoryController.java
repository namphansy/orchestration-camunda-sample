package com.example.inventory.api.controller;

import com.example.inventory.api.request.ReserveInventoryRequest;
import com.example.inventory.api.response.InventoryReservationResponse;
import com.example.inventory.application.service.InventoryReservationService;
import com.example.inventory.domain.exception.InsufficientStockException;
import com.example.inventory.api.request.RestockInventoryRequest;
import com.example.inventory.api.response.InventoryRestockResponse;
import com.example.inventory.application.service.InventoryRestockService;
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
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryReservationService reservationService;

    private final InventoryRestockService restockService;

    public InventoryController(
            InventoryReservationService reservationService,
            InventoryRestockService restockService
    ) {
        this.reservationService = reservationService;
        this.restockService = restockService;
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryReservationResponse reserve(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ReserveInventoryRequest request
    ) {
        return reservationService.reserve(request, idempotencyKey);
    }

    @PostMapping("/restocks")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryRestockResponse restock(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody RestockInventoryRequest request
    ) {
        return restockService.restock(request, idempotencyKey);
    }

    @GetMapping("/reservations/{reservationId}")
    public InventoryReservationResponse getReservation(@PathVariable("reservationId") String reservationId) {
        return reservationService.getReservation(reservationId);
    }

    @PostMapping("/reservations/{reservationId}/release")
    public InventoryReservationResponse release(
            @PathVariable("reservationId") String reservationId,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return reservationService.release(reservationId, idempotencyKey);
    }

    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleInsufficientStock(InsufficientStockException exception) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "service", "inventory-service",
                "correlationId", "",
                "errorCode", "INSUFFICIENT_STOCK",
                "message", exception.getMessage(),
                "details", Map.of()
        );
    }
}
