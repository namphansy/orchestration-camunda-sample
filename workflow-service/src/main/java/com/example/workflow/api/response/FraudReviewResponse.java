package com.example.workflow.api.response;

public record FraudReviewResponse(
        String reviewId,
        String orderId,
        String processInstanceId,
        String businessKey,
        String processState,
        String riskLevel,
        String reviewStatus,
        String fraudScore,
        String activeTaskKey,
        String activeTaskId,
        String startTime,
        String endTime
) {}
