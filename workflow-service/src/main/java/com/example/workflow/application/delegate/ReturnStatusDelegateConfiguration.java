package com.example.workflow.application.delegate;

import com.example.workflow.shared.ProcessVariables;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReturnStatusDelegateConfiguration {

    @Bean
    public JavaDelegate markReturnInspectionDelegate() {
        return execution -> execution.setVariable(
                ProcessVariables.RETURN_STATUS,
                "INSPECTION"
        );
    }

    @Bean
    public JavaDelegate markReturnAcceptedDelegate() {
        return execution -> execution.setVariable(
                ProcessVariables.RETURN_STATUS,
                "ACCEPTED"
        );
    }

    @Bean
    public JavaDelegate markReturnRejectedDelegate() {
        return execution -> {
            execution.setVariable(
                    ProcessVariables.RETURN_STATUS,
                    "REJECTED"
            );

            if (execution.getVariable(
                    ProcessVariables.FAILURE_REASON
            ) == null) {
                execution.setVariable(
                        ProcessVariables.FAILURE_REASON,
                        "Returned items did not pass inspection"
                );
            }
        };
    }

    @Bean
    public JavaDelegate markReturnExpiredDelegate() {
        return execution -> {
            execution.setVariable(
                    ProcessVariables.RETURN_STATUS,
                    "EXPIRED"
            );

            execution.setVariable(
                    ProcessVariables.FAILURE_REASON,
                    "Return item was not received within 7 days"
            );
        };
    }

    @Bean
    public JavaDelegate markReturnRefundedDelegate() {
        return execution -> execution.setVariable(
                ProcessVariables.RETURN_STATUS,
                "REFUNDED"
        );
    }
}
