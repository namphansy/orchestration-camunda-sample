package com.example.workflow.application.delegate;

import com.example.workflow.shared.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Random;

@Component
public class CalculateFraudScoreDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CalculateFraudScoreDelegate.class);
    private final Random random = new Random();

    @Override
    public void execute(DelegateExecution execution) {
        String businessKey = execution.getProcessBusinessKey();
        BigDecimal amount = (BigDecimal) execution.getVariable(ProcessVariables.ORDER_AMOUNT);

        int score;
        if (amount != null && amount.compareTo(BigDecimal.valueOf(1000)) >= 0) {
            score = 40 + random.nextInt(46); // rủi ro trung bình-cao: 40 - 85
        } else {
            score = 10 + random.nextInt(26); // rủi ro thấp: 10 - 35
        }

        log.info("[CalculateFraudScore] businessKey={} calculated fraudScore={}", businessKey, score);
        execution.setVariable(ProcessVariables.FRAUD_SCORE, score);
        execution.setVariable(ProcessVariables.REVIEW_ID, "FR-" + businessKey);
    }
}
