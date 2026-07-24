package com.example.workflow.api.request;

import javax.validation.constraints.NotBlank;

public record ClaimApprovalTaskRequest(
        @NotBlank String assignee
) {
}
