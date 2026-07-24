package com.example.workflow.application.service;

import com.example.workflow.api.request.ClaimApprovalTaskRequest;
import com.example.workflow.api.request.CompleteApprovalTaskRequest;
import com.example.workflow.api.response.ApprovalTaskResponse;
import com.example.workflow.shared.ProcessVariables;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.IdentityLink;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.springframework.stereotype.Service;

@Service
public class ApprovalTaskService {

    private static final List<String> APPROVAL_TASK_DEFINITION_KEYS = List.of(
            "ManagerApprovalTask",
            "DirectorApprovalTask"
    );

    private final TaskService taskService;
    private final RuntimeService runtimeService;

    public ApprovalTaskService(TaskService taskService, RuntimeService runtimeService) {
        this.taskService = taskService;
        this.runtimeService = runtimeService;
    }

    public List<ApprovalTaskResponse> listApprovalTasks(String candidateGroup) {
        TaskQuery query = taskService.createTaskQuery()
                .taskDefinitionKeyIn(APPROVAL_TASK_DEFINITION_KEYS.toArray(String[]::new))
                .active()
                .orderByTaskCreateTime()
                .asc();
        if (candidateGroup != null && !candidateGroup.isBlank()) {
            query.taskCandidateGroup(candidateGroup);
        }
        return query.list().stream()
                .map(this::toResponse)
                .toList();
    }

    public ApprovalTaskResponse claimTask(String taskId, ClaimApprovalTaskRequest request) {
        Task task = findApprovalTask(taskId);
        taskService.claim(taskId, request.assignee());
        return toResponse(findApprovalTask(taskId));
    }

    public void completeTask(String taskId, CompleteApprovalTaskRequest request) {
        Task task = findApprovalTask(taskId);
        Map<String, Object> variables = new HashMap<>();
        variables.put(ProcessVariables.APPROVED, request.approved());
        if (request.approver() != null && !request.approver().isBlank()) {
            variables.put(ProcessVariables.APPROVER, request.approver());
        } else if (task.getAssignee() != null && !task.getAssignee().isBlank()) {
            variables.put(ProcessVariables.APPROVER, task.getAssignee());
        }
        if (Boolean.FALSE.equals(request.approved())) {
            variables.put(ProcessVariables.FAILURE_REASON, "Order rejected during " + task.getName());
        }
        taskService.complete(taskId, variables);
    }

    private Task findApprovalTask(String taskId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .taskDefinitionKeyIn(APPROVAL_TASK_DEFINITION_KEYS.toArray(String[]::new))
                .active()
                .singleResult();
        if (task == null) {
            throw new ApprovalTaskNotFoundException(taskId);
        }
        return task;
    }

    private ApprovalTaskResponse toResponse(Task task) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult();
        Map<String, Object> variables = runtimeService.getVariables(task.getExecutionId());
        return new ApprovalTaskResponse(
                task.getId(),
                task.getName(),
                processInstance == null ? null : processInstance.getBusinessKey(),
                task.getProcessInstanceId(),
                stringValue(variables.get(ProcessVariables.ORDER_ID)),
                stringValue(variables.get(ProcessVariables.APPROVAL_LEVEL)),
                candidateGroup(task),
                task.getAssignee(),
                task.getCreateTime().toInstant().atOffset(ZoneOffset.UTC).toInstant()
        );
    }

    private String candidateGroup(Task task) {
        return taskService.getIdentityLinksForTask(task.getId()).stream()
                .map(IdentityLink::getGroupId)
                .filter(groupId -> groupId != null && !groupId.isBlank())
                .sorted(Comparator.naturalOrder())
                .findFirst()
                .orElse(null);
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
