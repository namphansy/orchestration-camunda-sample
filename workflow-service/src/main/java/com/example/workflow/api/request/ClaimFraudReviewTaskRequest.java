package com.example.workflow.api.request;

import javax.validation.constraints.NotBlank;

public record ClaimFraudReviewTaskRequest(
        @NotBlank(message = "assignee is required")
        String assignee
) {}
