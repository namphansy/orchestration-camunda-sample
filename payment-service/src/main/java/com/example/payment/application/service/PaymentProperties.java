package com.example.payment.application.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment")
public record PaymentProperties(
        Duration processingDelay,
        BigDecimal declineAbove,
        List<String> technicalFailureOrderIds
) {
    public PaymentProperties {
        if (processingDelay == null) {
            processingDelay = Duration.ZERO;
        }
        if (declineAbove == null) {
            declineAbove = new BigDecimal("1000.00");
        }
        if (technicalFailureOrderIds == null) {
            technicalFailureOrderIds = List.of();
        }
    }
}
