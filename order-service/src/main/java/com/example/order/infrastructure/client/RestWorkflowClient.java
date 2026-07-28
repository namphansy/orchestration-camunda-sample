package com.example.order.infrastructure.client;

import com.example.order.api.request.CreateOrderRequest;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestWorkflowClient implements WorkflowClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestWorkflowClient.class);

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
        LOGGER.info("Calling workflow-service to start order workflow. orderId={}, correlationId={}",
                request.orderId(), correlationId);
        WorkflowStartResponse response = restTemplate.postForObject(
                workflowBaseUrl + "/api/workflows/orders",
                workflowRequest,
                WorkflowStartResponse.class
        );
        if (response != null) {
            LOGGER.info("workflow-service accepted order workflow. orderId={}, correlationId={}, processInstanceId={}, status={}",
                    request.orderId(), correlationId, response.processInstanceId(), response.status());
        }
        return response;
    }
}
