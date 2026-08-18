package com.example.order.api.controller;

import com.example.order.application.service.OrderConflictException;
import com.example.order.application.service.OrderNotFoundException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OrderExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(
            OrderNotFoundException exception
    ) {
        return error("ORDER_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(OrderConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleConflict(
            OrderConflictException exception
    ) {
        return error("ORDER_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(
            IllegalArgumentException exception
    ) {
        return error("INVALID_ORDER_REQUEST", exception.getMessage());
    }

    private Map<String, Object> error(
            String errorCode,
            String message
    ) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "service", "order-service",
                "errorCode", errorCode,
                "message", message
        );
    }
}