package com.example.workflow.application.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.workflow.domain.model.FraudReviewEntity;
import com.example.workflow.domain.repository.FraudReviewRepository;
import com.example.workflow.shared.ProcessVariables;

@Service
public class FraudReviewService {

    private static final Logger log = LoggerFactory.getLogger(FraudReviewService.class);
    private static final String PROCESS_DEFINITION_KEY = "fraud-review";

    public static final String TASK_FRAUD_ANALYST_REVIEW = "FraudAnalystReview";
    public static final String TASK_FRAUD_MANAGER_REVIEW = "FraudManagerReview";

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final TaskService    taskService;
    private final FraudReviewRepository fraudReviewRepository;

    public FraudReviewService(RuntimeService runtimeService,
                               HistoryService historyService,
                               TaskService taskService,
                               FraudReviewRepository fraudReviewRepository) {
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.taskService    = taskService;
        this.fraudReviewRepository = fraudReviewRepository;
    }

    public String startFraudReview(String businessKey, Map<String, Object> variables) {
        log.info("[FraudReview] Starting process businessKey={}", businessKey);

        ProcessInstance existing = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(PROCESS_DEFINITION_KEY)
                .processInstanceBusinessKey(businessKey)
                .singleResult();
        if (existing != null) {
            throw new IllegalStateException(
                    "Fraud review already exists for businessKey: " + businessKey);
        }

        String reviewId = (String) variables.get(ProcessVariables.REVIEW_ID);
        String orderId = (String) variables.get(ProcessVariables.ORDER_ID);
        String customerId = (String) variables.get(ProcessVariables.CUSTOMER_ID);
        Object rawAmount = variables.get(ProcessVariables.ORDER_AMOUNT);
        BigDecimal orderAmount = rawAmount instanceof BigDecimal bd ? bd : new BigDecimal(String.valueOf(rawAmount));
        Integer fraudScore = (Integer) variables.get(ProcessVariables.FRAUD_SCORE);
        String correlationId = (String) variables.get(ProcessVariables.CORRELATION_ID);

        FraudReviewEntity entity = new FraudReviewEntity(
                reviewId != null ? reviewId : businessKey,
                orderId, customerId, orderAmount, fraudScore, correlationId
        );

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                PROCESS_DEFINITION_KEY, businessKey, variables);
        log.info("[FraudReview] Started processInstanceId={}", instance.getId());

        entity.setProcessInstanceId(instance.getId());

        String riskLevel = (String) runtimeService.getVariable(instance.getId(), ProcessVariables.RISK_LEVEL);
        if (riskLevel != null) {
            entity.setRiskLevel(riskLevel);
        }

        fraudReviewRepository.save(entity);

        return instance.getId();
    }

    public FraudReviewStatus getStatus(String businessKey) {
        Optional<FraudReviewEntity> entityOpt = fraudReviewRepository.findById(businessKey);

        ProcessInstance active = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(PROCESS_DEFINITION_KEY)
                .processInstanceBusinessKey(businessKey)
                .singleResult();

        if (active != null) {
            return buildActiveStatus(active, entityOpt.orElse(null));
        }

        HistoricProcessInstance historic = historyService
                .createHistoricProcessInstanceQuery()
                .processDefinitionKey(PROCESS_DEFINITION_KEY)
                .processInstanceBusinessKey(businessKey)
                .singleResult();

        if (historic == null && entityOpt.isEmpty()) {
            throw new FraudReviewNotFoundException(businessKey);
        }
        return buildHistoricStatus(historic, entityOpt.orElse(null), businessKey);
    }

    private FraudReviewStatus buildActiveStatus(ProcessInstance instance, FraudReviewEntity entity) {
        String pid = instance.getId();
        Task activeTask = taskService.createTaskQuery()
                .processInstanceId(pid).active().singleResult();

        String riskLevel = (String) runtimeService.getVariable(pid, ProcessVariables.RISK_LEVEL);
        if (entity != null && riskLevel != null && entity.getRiskLevel() == null) {
            entity.setRiskLevel(riskLevel);
            fraudReviewRepository.save(entity);
        }

        return new FraudReviewStatus(
                entity != null ? entity.getReviewId() : (String) runtimeService.getVariable(pid, ProcessVariables.REVIEW_ID),
                entity != null ? entity.getOrderId() : (String) runtimeService.getVariable(pid, ProcessVariables.ORDER_ID),
                pid,
                instance.getBusinessKey(),
                "ACTIVE",
                riskLevel != null ? riskLevel : (entity != null ? entity.getRiskLevel() : null),
                entity != null ? entity.getReviewStatus() : (String) runtimeService.getVariable(pid, ProcessVariables.REVIEW_STATUS),
                entity != null ? String.valueOf(entity.getFraudScore()) : String.valueOf(runtimeService.getVariable(pid, ProcessVariables.FRAUD_SCORE)),
                activeTask != null ? activeTask.getTaskDefinitionKey() : null,
                activeTask != null ? activeTask.getId() : null,
                entity != null && entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null,
                null
        );
    }

    private FraudReviewStatus buildHistoricStatus(HistoricProcessInstance historic, FraudReviewEntity entity, String businessKey) {
        String pid = historic != null ? historic.getId() : (entity != null ? entity.getProcessInstanceId() : null);
        return new FraudReviewStatus(
                entity != null ? entity.getReviewId() : getHistoricVar(pid, ProcessVariables.REVIEW_ID),
                entity != null ? entity.getOrderId() : getHistoricVar(pid, ProcessVariables.ORDER_ID),
                pid,
                historic != null ? historic.getBusinessKey() : businessKey,
                historic != null ? historic.getState() : "COMPLETED",
                entity != null && entity.getRiskLevel() != null ? entity.getRiskLevel() : getHistoricVar(pid, ProcessVariables.RISK_LEVEL),
                entity != null && entity.getReviewStatus() != null ? entity.getReviewStatus() : getHistoricVar(pid, ProcessVariables.REVIEW_STATUS),
                entity != null ? String.valueOf(entity.getFraudScore()) : getHistoricVar(pid, ProcessVariables.FRAUD_SCORE),
                null,
                null,
                historic != null && historic.getStartTime() != null ? historic.getStartTime().toString() : (entity != null ? entity.getCreatedAt().toString() : null),
                historic != null && historic.getEndTime() != null ? historic.getEndTime().toString() : (entity != null ? entity.getUpdatedAt().toString() : null)
        );
    }

    private String getHistoricVar(String pid, String name) {
        if (pid == null) return null;
        HistoricVariableInstance hvi = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(pid).variableName(name).singleResult();
        return hvi != null ? String.valueOf(hvi.getValue()) : null;
    }

    public List<Task> listActiveTasks() {
        return taskService.createTaskQuery()
                .processDefinitionKey(PROCESS_DEFINITION_KEY)
                .taskDefinitionKeyIn(TASK_FRAUD_ANALYST_REVIEW, TASK_FRAUD_MANAGER_REVIEW)
                .active()
                .orderByTaskCreateTime().asc()
                .list();
    }

    public void claimTask(String taskId, String assignee) {
        Task task = findActiveTask(taskId);
        log.info("[FraudReview] Claim taskId={} assignee={}", taskId, assignee);
        taskService.claim(task.getId(), assignee);

        String pid = task.getProcessInstanceId();
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(pid).singleResult();
        if (instance != null && instance.getBusinessKey() != null) {
            fraudReviewRepository.findById(instance.getBusinessKey()).ifPresent(entity -> {
                entity.setAssignee(assignee);
                fraudReviewRepository.save(entity);
            });
        }
    }

    public void completeTask(String taskId, boolean approved, String approver, String comment, String reason) {
        Task task = findActiveTask(taskId);
        log.info("[FraudReview] Complete taskId={} approved={} approver={}", taskId, approved, approver);

        Map<String, Object> vars = new HashMap<>();
        vars.put(ProcessVariables.APPROVED, approved);
        String finalStatus = approved ? ProcessVariables.STATUS_APPROVED : ProcessVariables.STATUS_REJECTED;
        vars.put(ProcessVariables.REVIEW_STATUS, finalStatus);
        
        String effectiveComment = (comment != null && !comment.isBlank()) ? comment : reason;

        if (approver != null && !approver.isBlank()) {
            vars.put(ProcessVariables.APPROVER, approver);
        }
        if (!approved && effectiveComment != null && !effectiveComment.isBlank()) {
            vars.put(ProcessVariables.FAILURE_REASON, effectiveComment);
        }

        String pid = task.getProcessInstanceId();
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(pid).singleResult();
        if (instance != null && instance.getBusinessKey() != null) {
            fraudReviewRepository.findById(instance.getBusinessKey()).ifPresent(entity -> {
                entity.setReviewStatus(finalStatus);
                if (approver != null && !approver.isBlank()) {
                    entity.setApprover(approver);
                } else if (task.getAssignee() != null) {
                    entity.setApprover(task.getAssignee());
                }
                entity.setComment(effectiveComment);
                if (!approved) {
                    entity.setFailureReason(effectiveComment);
                }
                fraudReviewRepository.save(entity);
            });
        }

        taskService.complete(task.getId(), vars);
    }

    public void completeTask(String taskId, boolean approved, String reason) {
        completeTask(taskId, approved, null, reason, reason);
    }

    private Task findActiveTask(String taskId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .processDefinitionKey(PROCESS_DEFINITION_KEY)
                .active()
                .singleResult();
        if (task == null) {
            throw new FraudReviewTaskNotFoundException(taskId);
        }
        return task;
    }

    public record FraudReviewStatus(
            String reviewId,
            String orderId,
            String processInstanceId,
            String businessKey,
            String processState,
            String riskLevel,
            String reviewStatus,
            String fraudScore,
            String activeTaskKey,
            String activeTaskId,
            String startTime,
            String endTime
    ) {}
}
