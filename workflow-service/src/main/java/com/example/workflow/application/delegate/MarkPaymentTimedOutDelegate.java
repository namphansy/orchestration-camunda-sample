package com.example.workflow.application.delegate;

import com.example.workflow.shared.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MarkPaymentTimedOutDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarkPaymentTimedOutDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        LOGGER.info("Payment confirmation timed out. businessKey={}, correlationId={}",
                execution.getBusinessKey(),
                execution.getVariable(ProcessVariables.CORRELATION_ID));
        execution.setVariable(ProcessVariables.PAYMENT_STATUS, "TIMED_OUT");
        execution.setVariable(ProcessVariables.PAYMENT_CONFIRMED, false);
        execution.setVariable(ProcessVariables.FAILURE_REASON, "Payment confirmation timed out");
    }
}
