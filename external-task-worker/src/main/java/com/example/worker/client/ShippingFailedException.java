package com.example.worker.client;

public class ShippingFailedException extends RuntimeException {

    public ShippingFailedException(String message) {
        super(message);
    }
}
