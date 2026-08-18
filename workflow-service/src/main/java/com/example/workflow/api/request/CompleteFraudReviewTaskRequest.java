package com.example.workflow.api.request;

import javax.validation.constraints.NotNull;

public record CompleteFraudReviewTaskRequest(
        @NotNull(message = "approved is required")
        Boolean approved,

        String approver,

        String comment,

        String reason
) {
    public String getEffectiveComment() {
        if (comment != null && !comment.isBlank()) {
            return comment;
        }
        return reason;
    }
}
