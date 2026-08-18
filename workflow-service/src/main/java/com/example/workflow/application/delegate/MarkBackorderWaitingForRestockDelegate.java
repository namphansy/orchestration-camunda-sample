package com.example.workflow.application.delegate;

import com.example.workflow.shared.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MarkBackorderWaitingForRestockDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarkBackorderWaitingForRestockDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        String businessKey = execution.getBusinessKey();
        execution.setVariable(ProcessVariables.BACKORDER_STATUS, "WAITING_FOR_RESTOCK");
        LOGGER.info("Backorder marked as WAITING_FOR_RESTOCK. businessKey={}", businessKey);
    }
}
