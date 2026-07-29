package com.example.worker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "worker.subscriptions-enabled=false")
class ExternalTaskWorkerApplicationTests {

    @Test
    void contextLoads() {
    }
}
