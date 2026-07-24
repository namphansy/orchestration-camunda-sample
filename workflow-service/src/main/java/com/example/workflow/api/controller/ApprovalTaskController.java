package com.example.workflow.api.controller;

import com.example.workflow.api.request.ClaimApprovalTaskRequest;
import com.example.workflow.api.request.CompleteApprovalTaskRequest;
import com.example.workflow.api.response.ApprovalTaskResponse;
import com.example.workflow.application.service.ApprovalTaskService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows/orders/tasks")
public class ApprovalTaskController {

    private final ApprovalTaskService approvalTaskService;

    public ApprovalTaskController(ApprovalTaskService approvalTaskService) {
        this.approvalTaskService = approvalTaskService;
    }

    @GetMapping
    public List<ApprovalTaskResponse> listApprovalTasks(@RequestParam(required = false) String candidateGroup) {
        return approvalTaskService.listApprovalTasks(candidateGroup);
    }

    @PostMapping("/{taskId}/claim")
    public ApprovalTaskResponse claimTask(
            @PathVariable String taskId,
            @Valid @RequestBody ClaimApprovalTaskRequest request
    ) {
        return approvalTaskService.claimTask(taskId, request);
    }

    @PostMapping("/{taskId}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void completeTask(
            @PathVariable String taskId,
            @Valid @RequestBody CompleteApprovalTaskRequest request
    ) {
        approvalTaskService.completeTask(taskId, request);
    }
}
