package com.example.worker.task;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.worker.config.WorkerProperties;
import java.time.Duration;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;

class WorkerFailureHandlerTests {

    @Test
    void reportsFailureDetailsAndDecrementsRetries() {
        WorkerProperties properties = new WorkerProperties();
        properties.setDefaultRetries(3);
        properties.setRetryTimeout(Duration.ofSeconds(15));
        WorkerFailureHandler handler = new WorkerFailureHandler(properties);
        ExternalTask task = org.mockito.Mockito.mock(ExternalTask.class);
        ExternalTaskService service = org.mockito.Mockito.mock(ExternalTaskService.class);
        RuntimeException exception = new IllegalStateException("Payment processor unavailable");
        when(task.getRetries()).thenReturn(2);

        handler.handleRetryableFailure(task, service, exception);

        verify(service).handleFailure(
                org.mockito.ArgumentMatchers.eq(task),
                org.mockito.ArgumentMatchers.eq("Payment processor unavailable"),
                org.mockito.ArgumentMatchers.contains("Payment processor unavailable"),
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq(15_000L)
        );
    }
}
