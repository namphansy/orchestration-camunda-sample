package com.example.workflow.api.controller;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.workflow.api.request.ClaimFraudReviewTaskRequest;
import com.example.workflow.api.request.CompleteFraudReviewTaskRequest;
import com.example.workflow.api.request.StartFraudReviewRequest;
import com.example.workflow.api.response.FraudReviewResponse;
import com.example.workflow.api.response.FraudReviewTaskResponse;
import com.example.workflow.application.service.FraudReviewNotFoundException;
import com.example.workflow.application.service.FraudReviewService;
import com.example.workflow.application.service.FraudReviewTaskNotFoundException;
import com.example.workflow.shared.ProcessVariables;

@RestController
@RequestMapping("/api/workflows/fraud-reviews")
public class FraudReviewController {

    private final FraudReviewService fraudReviewService;
    private final RuntimeService     runtimeService;

    public FraudReviewController(FraudReviewService fraudReviewService,
                                  RuntimeService runtimeService) {
        this.fraudReviewService = fraudReviewService;
        this.runtimeService     = runtimeService;
    }



    @PostMapping
    public ResponseEntity<Map<String, String>> startFraudReview(
            @Valid @RequestBody StartFraudReviewRequest request) {

        String businessKey = request.reviewId();

        Map<String, Object> variables = Map.of(
                ProcessVariables.REVIEW_ID,     request.reviewId(),
                ProcessVariables.ORDER_ID,       request.orderId(),
                ProcessVariables.CUSTOMER_ID,    request.customerId(),
                ProcessVariables.ORDER_AMOUNT,   request.orderAmount(),
                ProcessVariables.FRAUD_SCORE,    request.fraudScore(),
                ProcessVariables.CORRELATION_ID,
                        request.correlationId() != null ? request.correlationId() : request.reviewId()
        );

        String processInstanceId = fraudReviewService.startFraudReview(businessKey, variables);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "processInstanceId", processInstanceId,
                "businessKey",       businessKey,
                "message",           "Fraud review started"
        ));
    }


    @GetMapping("/{businessKey}")
    public ResponseEntity<FraudReviewResponse> getFraudReview(
            @PathVariable String businessKey) {

        FraudReviewService.FraudReviewStatus status = fraudReviewService.getStatus(businessKey);
        return ResponseEntity.ok(toResponse(status));
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<FraudReviewTaskResponse>> listTasks() {
        List<FraudReviewTaskResponse> response = fraudReviewService.listActiveTasks()
                .stream()
                .map(this::toTaskResponse)
                .toList();
        return ResponseEntity.ok(response);
    }


    @PostMapping("/tasks/{taskId}/claim")
    public ResponseEntity<Map<String, String>> claimTask(
            @PathVariable String taskId,
            @Valid @RequestBody ClaimFraudReviewTaskRequest request) {

        fraudReviewService.claimTask(taskId, request.assignee());
        return ResponseEntity.ok(Map.of(
                "taskId",   taskId,
                "assignee", request.assignee(),
                "message",  "Task claimed"
        ));
    }

 
    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<Void> completeTask(
            @PathVariable String taskId,
            @Valid @RequestBody CompleteFraudReviewTaskRequest request) {

        String effectiveComment = request.getEffectiveComment();
        if (Boolean.FALSE.equals(request.approved())
                && (effectiveComment == null || effectiveComment.isBlank())) {
            return ResponseEntity.badRequest().build();
        }

        fraudReviewService.completeTask(
                taskId,
                request.approved(),
                request.approver(),
                request.comment(),
                request.reason()
        );
        return ResponseEntity.noContent().build();
    }



    @ExceptionHandler(FraudReviewNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(FraudReviewNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(FraudReviewTaskNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleTaskNotFound(FraudReviewTaskNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }


    private FraudReviewResponse toResponse(FraudReviewService.FraudReviewStatus s) {
        return new FraudReviewResponse(
                s.reviewId(), s.orderId(),
                s.processInstanceId(), s.businessKey(),
                s.processState(), s.riskLevel(), s.reviewStatus(),
                s.fraudScore(), s.activeTaskKey(), s.activeTaskId(),
                s.startTime(), s.endTime()
        );
    }

    private FraudReviewTaskResponse toTaskResponse(Task task) {
        String pid = task.getProcessInstanceId();
        String businessKey = runtimeService.createProcessInstanceQuery()
                .processInstanceId(pid).singleResult() != null
                ? runtimeService.createProcessInstanceQuery()
                        .processInstanceId(pid).singleResult().getBusinessKey()
                : null;
        return new FraudReviewTaskResponse(
                task.getId(),
                task.getTaskDefinitionKey(),
                pid,
                businessKey,
                task.getAssignee(),
                task.getCreateTime() != null ? task.getCreateTime().toString() : null,
                (String) runtimeService.getVariable(pid, ProcessVariables.REVIEW_ID),
                (String) runtimeService.getVariable(pid, ProcessVariables.ORDER_ID),
                (String) runtimeService.getVariable(pid, ProcessVariables.RISK_LEVEL)
        );
    }
}
