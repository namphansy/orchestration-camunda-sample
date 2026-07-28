package com.example.worker.task;

import com.example.worker.config.WorkerProperties;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

@Component
public class WorkerFailureHandler {

    private final WorkerProperties properties;

    public WorkerFailureHandler(WorkerProperties properties) {
        this.properties = properties;
    }

    public void handleRetryableFailure(ExternalTask task, ExternalTaskService service, RuntimeException exception) {
        int retries = task.getRetries() == null ? properties.getDefaultRetries() : task.getRetries();
        int remainingRetries = Math.max(retries - 1, 0);
        service.handleFailure(
                task,
                exception.getMessage(),
                stackTrace(exception),
                remainingRetries,
                properties.getRetryTimeout().toMillis()
        );
    }

    private String stackTrace(RuntimeException exception) {
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
