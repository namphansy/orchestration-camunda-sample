package com.example.worker.config;

import org.camunda.bpm.client.ExternalTaskClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExternalTaskClientConfig {

    @Bean
    ExternalTaskClient externalTaskClient(WorkerProperties properties) {
        return ExternalTaskClient.create()
                .baseUrl(properties.getCamundaBaseUrl())
                .workerId(properties.getWorkerId())
                .asyncResponseTimeout(properties.getAsyncResponseTimeout().toMillis())
                .build();
    }
}
