package com.example.workflow.application.delegate;

import com.example.workflow.domain.repository.FraudReviewRepository;
import com.example.workflow.shared.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ApproveReviewDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ApproveReviewDelegate.class);

    private final FraudReviewRepository fraudReviewRepository;

    public ApproveReviewDelegate(FraudReviewRepository fraudReviewRepository) {
        this.fraudReviewRepository = fraudReviewRepository;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String businessKey = execution.getProcessBusinessKey();
        String riskLevel = (String) execution.getVariable(ProcessVariables.RISK_LEVEL);
        log.info("[ApproveReview] businessKey={} auto-approved (LOW risk)", businessKey);
        
        execution.setVariable(ProcessVariables.APPROVED, true);
        execution.setVariable(ProcessVariables.REVIEW_STATUS, ProcessVariables.STATUS_APPROVED);

        fraudReviewRepository.findById(businessKey).ifPresent(entity -> {
            entity.setRiskLevel(riskLevel != null ? riskLevel : "LOW");
            entity.setReviewStatus(ProcessVariables.STATUS_APPROVED);
            entity.setApprover("SYSTEM");
            entity.setComment("Auto-approved by DMN rule (Low Risk)");
            fraudReviewRepository.save(entity);
        });
    }
}
