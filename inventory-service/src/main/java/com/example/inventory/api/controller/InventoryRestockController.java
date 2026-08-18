package com.example.inventory.api.controller;

import com.example.inventory.api.request.RestockInventoryRequest;
import com.example.inventory.application.service.InventoryReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryRestockController {

    private final InventoryReservationService reservationService;

    public InventoryRestockController(InventoryReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/restocks")
    @ResponseStatus(HttpStatus.OK)
    public void restock(@Valid @RequestBody RestockInventoryRequest request) {
        reservationService.restock(request);
    }
}
