package com.example.worker.client;

public class PaymentDeclinedException extends RuntimeException {

    public PaymentDeclinedException(String message) {
        super(message);
    }
}
