package com.example.workflow.api.request;

import javax.validation.constraints.NotNull;

public record CompleteApprovalTaskRequest(
        @NotNull Boolean approved,
        String approver
) {
}
