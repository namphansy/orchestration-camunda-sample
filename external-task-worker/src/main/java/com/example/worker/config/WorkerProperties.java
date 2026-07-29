package com.example.worker.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "worker")
public class WorkerProperties {

    private String camundaBaseUrl = "http://localhost:8080/engine-rest";
    private String workerId = "order-external-task-worker";
    private Duration asyncResponseTimeout = Duration.ofSeconds(20);
    private Duration lockDuration = Duration.ofMinutes(1);
    private int defaultRetries = 3;
    private Duration retryTimeout = Duration.ofSeconds(30);
    private boolean subscriptionsEnabled = true;

    public String getCamundaBaseUrl() {
        return camundaBaseUrl;
    }

    public void setCamundaBaseUrl(String camundaBaseUrl) {
        this.camundaBaseUrl = camundaBaseUrl;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public Duration getAsyncResponseTimeout() {
        return asyncResponseTimeout;
    }

    public void setAsyncResponseTimeout(Duration asyncResponseTimeout) {
        this.asyncResponseTimeout = asyncResponseTimeout;
    }

    public Duration getLockDuration() {
        return lockDuration;
    }

    public void setLockDuration(Duration lockDuration) {
        this.lockDuration = lockDuration;
    }

    public int getDefaultRetries() {
        return defaultRetries;
    }

    public void setDefaultRetries(int defaultRetries) {
        this.defaultRetries = defaultRetries;
    }

    public Duration getRetryTimeout() {
        return retryTimeout;
    }

    public void setRetryTimeout(Duration retryTimeout) {
        this.retryTimeout = retryTimeout;
    }

    public boolean isSubscriptionsEnabled() {
        return subscriptionsEnabled;
    }

    public void setSubscriptionsEnabled(boolean subscriptionsEnabled) {
        this.subscriptionsEnabled = subscriptionsEnabled;
    }
}
