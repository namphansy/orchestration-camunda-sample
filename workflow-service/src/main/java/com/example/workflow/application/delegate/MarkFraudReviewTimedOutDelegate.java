package com.example.workflow.application.delegate;

import com.example.workflow.domain.repository.FraudReviewRepository;
import com.example.workflow.shared.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MarkFraudReviewTimedOutDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(MarkFraudReviewTimedOutDelegate.class);

    private final FraudReviewRepository fraudReviewRepository;

    public MarkFraudReviewTimedOutDelegate(FraudReviewRepository fraudReviewRepository) {
        this.fraudReviewRepository = fraudReviewRepository;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String businessKey = execution.getProcessBusinessKey();
        String riskLevel   = (String) execution.getVariable(ProcessVariables.RISK_LEVEL);
        log.warn("[FraudReview] TIMEOUT businessKey={} riskLevel={}", businessKey, riskLevel);

        String failureReason = "Fraud review timed out waiting for " + resolveReviewer(riskLevel) + " decision";

        execution.setVariable(ProcessVariables.APPROVED,       false);
        execution.setVariable(ProcessVariables.REVIEW_STATUS,  ProcessVariables.STATUS_TIMED_OUT);
        execution.setVariable(ProcessVariables.FAILURE_REASON, failureReason);

        fraudReviewRepository.findById(businessKey).ifPresent(entity -> {
            entity.setRiskLevel(riskLevel);
            entity.setReviewStatus(ProcessVariables.STATUS_TIMED_OUT);
            entity.setFailureReason(failureReason);
            fraudReviewRepository.save(entity);
        });
    }

    private String resolveReviewer(String riskLevel) {
        return "HIGH".equals(riskLevel) ? "fraud manager" : "fraud analyst";
    }
}
