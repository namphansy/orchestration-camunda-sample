package com.example.workflow.application.delegate;

import com.example.workflow.shared.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MarkBackorderExpiredDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarkBackorderExpiredDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        String businessKey = execution.getBusinessKey();
        execution.setVariable(ProcessVariables.BACKORDER_STATUS, "EXPIRED");
        LOGGER.info("Backorder marked as EXPIRED. businessKey={}", businessKey);
    }
}
