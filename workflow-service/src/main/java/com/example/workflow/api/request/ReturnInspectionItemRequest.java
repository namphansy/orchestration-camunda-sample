package com.example.workflow.api.request;

import java.io.Serializable;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public record ReturnInspectionItemRequest(
        @NotBlank String sku,
        @NotNull @Min(1) Integer quantity,
        @NotNull Boolean restockable
) implements Serializable {
}