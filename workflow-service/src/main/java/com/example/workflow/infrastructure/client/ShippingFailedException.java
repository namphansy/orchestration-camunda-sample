package com.example.workflow.infrastructure.client;

public class ShippingFailedException extends RuntimeException {

    public ShippingFailedException(String message) {
        super(message);
    }
}
