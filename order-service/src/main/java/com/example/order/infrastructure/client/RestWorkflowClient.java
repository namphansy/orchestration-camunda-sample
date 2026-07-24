package com.example.order.infrastructure.client;

import com.example.order.api.request.CreateOrderRequest;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestWorkflowClient implements WorkflowClient {

    private final RestTemplate restTemplate;
    private final String workflowBaseUrl;

    public RestWorkflowClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${workflow.service.base-url}") String workflowBaseUrl
    ) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.workflowBaseUrl = workflowBaseUrl;
    }

    @Override
    public WorkflowStartResponse startOrderWorkflow(CreateOrderRequest request, String correlationId) {
        WorkflowStartRequest workflowRequest = new WorkflowStartRequest(
                request.orderId(),
                correlationId
        );
        return restTemplate.postForObject(
                workflowBaseUrl + "/api/workflows/orders",
                workflowRequest,
                WorkflowStartResponse.class
        );
    }
}
