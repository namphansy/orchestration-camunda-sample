package com.example.workflow.api.controller;

import com.example.workflow.api.request.StartOrderWorkflowRequest;
import com.example.workflow.api.response.OrderWorkflowResponse;
import com.example.workflow.application.service.OrderWorkflowService;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows/orders")
public class OrderWorkflowController {

    private final OrderWorkflowService orderWorkflowService;

    public OrderWorkflowController(OrderWorkflowService orderWorkflowService) {
        this.orderWorkflowService = orderWorkflowService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderWorkflowResponse startOrderWorkflow(@Valid @RequestBody StartOrderWorkflowRequest request) {
        return orderWorkflowService.startOrderWorkflow(request);
    }

    @GetMapping("/{businessKey}")
    public OrderWorkflowResponse getOrderWorkflow(@PathVariable String businessKey) {
        return orderWorkflowService.getOrderWorkflow(businessKey);
    }
}

