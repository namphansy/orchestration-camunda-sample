package com.example.shipping.api.controller;

import com.example.shipping.api.request.CreateReturnShipmentRequest;
import com.example.shipping.api.response.ReturnShipmentResponse;
import com.example.shipping.application.service.ReturnShipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/return-shipments")
public class ReturnShipmentController {

    private final ReturnShipmentService returnShipmentService;

    public ReturnShipmentController(
            ReturnShipmentService returnShipmentService
    ) {
        this.returnShipmentService = returnShipmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnShipmentResponse create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateReturnShipmentRequest request
    ) {
        return returnShipmentService.create(request, idempotencyKey);
    }

    @GetMapping("/{returnShipmentId}")
    public ReturnShipmentResponse get(
            @PathVariable("returnShipmentId") String returnShipmentId
    ) {
        return returnShipmentService.get(returnShipmentId);
    }
}