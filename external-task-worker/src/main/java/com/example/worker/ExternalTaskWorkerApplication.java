package com.example.worker;

import com.example.worker.config.WorkerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(WorkerProperties.class)
public class ExternalTaskWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExternalTaskWorkerApplication.class, args);
    }
}
