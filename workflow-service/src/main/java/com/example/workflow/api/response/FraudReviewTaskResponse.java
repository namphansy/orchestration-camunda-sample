package com.example.workflow.api.response;

public record FraudReviewTaskResponse(
        String taskId,
        String taskDefinitionKey,
        String processInstanceId,
        String businessKey,
        String assignee,
        String createTime,
        String reviewId,
        String orderId,
        String riskLevel
) {}
