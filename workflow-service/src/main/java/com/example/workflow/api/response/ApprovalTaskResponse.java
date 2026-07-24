package com.example.workflow.api.response;

import java.time.Instant;

public record ApprovalTaskResponse(
        String taskId,
        String name,
        String businessKey,
        String processInstanceId,
        String orderId,
        String approvalLevel,
        String candidateGroup,
        String assignee,
        Instant createdAt
) {
}
