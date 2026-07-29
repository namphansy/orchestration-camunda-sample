package com.example.workflow.application.delegate;

import com.example.workflow.shared.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ApprovalTimeoutDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApprovalTimeoutDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        Object approvalLevel = execution.getVariable(ProcessVariables.APPROVAL_LEVEL);
        String reason = "Approval timed out for level " + approvalLevel;
        execution.setVariable(ProcessVariables.APPROVED, false);
        execution.setVariable(ProcessVariables.FAILURE_REASON, reason);
        LOGGER.info("Approval timed out. businessKey={}, correlationId={}, approvalLevel={}",
                execution.getBusinessKey(),
                execution.getVariable(ProcessVariables.CORRELATION_ID),
                approvalLevel);
    }
}