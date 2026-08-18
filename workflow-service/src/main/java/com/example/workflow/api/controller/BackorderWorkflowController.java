package com.example.workflow.api.controller;

import com.example.workflow.api.request.RestockEventRequest;
import com.example.workflow.api.response.BackorderWorkflowResponse;
import com.example.workflow.api.response.RestockEventResponse;
import com.example.workflow.application.service.BackorderWorkflowService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows/backorders")
public class BackorderWorkflowController {

    private final BackorderWorkflowService backorderWorkflowService;

    public BackorderWorkflowController(BackorderWorkflowService backorderWorkflowService) {
        this.backorderWorkflowService = backorderWorkflowService;
    }

    @GetMapping("/{businessKey}")
    public BackorderWorkflowResponse getBackorderWorkflow(@PathVariable String businessKey) {
        return backorderWorkflowService.getBackorderWorkflow(businessKey);
    }

    @PostMapping("/{businessKey}/restock-events")
    public RestockEventResponse handleRestockEvent(
            @PathVariable String businessKey,
            @Valid @RequestBody RestockEventRequest request
    ) {
        return backorderWorkflowService.handleRestockEvent(businessKey, request);
    }
}
