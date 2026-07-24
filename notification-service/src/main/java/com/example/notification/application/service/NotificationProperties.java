package com.example.notification.application.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {

    private List<String> failOrderIds = new ArrayList<>();

    public List<String> getFailOrderIds() {
        return failOrderIds;
    }

    public void setFailOrderIds(List<String> failOrderIds) {
        this.failOrderIds = failOrderIds;
    }
}
