package com.example.worker;

import org.camunda.bpm.client.ExternalTaskClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = "worker.subscriptions-enabled=false")
class ExternalTaskWorkerApplicationTests {

    @MockBean
    private ExternalTaskClient externalTaskClient;

    @Test
    void contextLoads() {
    }
}
