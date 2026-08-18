package com.example.workflow.api.request;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public record ReturnInspectionRequest(
        @NotNull Boolean inspectionAccepted,
        @NotBlank String inspector,
        String comment,
        @NotEmpty List<@Valid ReturnInspectionItemRequest> items,
        @NotBlank String correlationId
) {
}