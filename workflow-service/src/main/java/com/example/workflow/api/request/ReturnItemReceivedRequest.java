package com.example.workflow.api.request;

import java.time.Instant;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public record ReturnItemReceivedRequest(
        @NotBlank String receivedBy,
        @NotNull Instant receivedAt,
        @NotBlank String correlationId
) {
}