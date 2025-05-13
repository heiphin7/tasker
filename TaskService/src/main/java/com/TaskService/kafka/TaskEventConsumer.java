package com.TaskService.kafka;

import com.TaskService.dto.CreateSubtaskDto;
import com.TaskService.dto.CreateTeamTaskDto;
import com.TaskService.json.TaskSerializer;
import com.TaskService.model.Subtask;
import com.TaskService.model.Task;
import com.TaskService.model.TeamTask;
import com.TaskService.model.kafkaModels.KafkaRequest;
import com.TaskService.repository.TaskRepository;
import com.TaskService.service.TaskService;
import com.TaskService.service.TeamTaskService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskEventConsumer {

    private final TaskService taskService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TaskRepository taskRepository;
    private final TeamTaskService teamTaskService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "task-update", groupId = "task-group")
    public void handleTaskUpdate(String message) {
        try {
            message = message.substring(1, message.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");

            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);
            Task task = Task.fromJson(kafkaRequest.getTask());

            String requestId = kafkaRequest.getRequestId();
            Long userId = kafkaRequest.getUserId();
            Long taskToChangeId = kafkaRequest.getTaskToChangeId();

            Task taskInDatabase = taskService.getTaskById(taskToChangeId);

            if (!taskInDatabase.getCreatorId().equals(userId)) {
                sendErrorMessage("task-update-response" ,requestId, "Вы не можете изменить чужую задачу");
                return;
            }
            taskService.updateTask(task, taskInDatabase.getId());
            sendSuccessMessage("task-update-response", requestId);
        } catch (Exception e) {
            log.error("Произошла ошибка при попытке обновить задачу", e);
        }
    }

    @KafkaListener(topics = "task-delete")
    @SneakyThrows
    public void deleteTask(String message) {
        String requestId = "";
        Map<String, Object> response = new HashMap<>();

        try {
            message = message.substring(1, message.length() - 1);
            message = message.replace("\\\"", "\"");

            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);
            requestId = kafkaRequest.getRequestId();
            Long taskId = kafkaRequest.getTaskId();
            Long userId = kafkaRequest.getUserId();

            taskService.deleteTask(userId, taskId);
            sendSuccessMessage("task-delete-response", requestId);
        } catch (Exception e) {
            log.error("Ошибка при удалении задачи: " + e.getMessage());
            response.put("requestId", requestId);
            response.put("response-message", "ERROR:" + e.getMessage());
            kafkaTemplate.send("task-delete-response", objectMapper.writeValueAsString(response));
        }
    }

    @KafkaListener(topics = "team-create-task", groupId = "task-group")
    public void createTeamTask(String message) {
        String requestId = "";

        try {
            message = message.substring(1, message.length() - 1).replace("\\\"", "\"");
            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);
            requestId = kafkaRequest.getRequestId();

            Long userId = kafkaRequest.getUserId();
            Long teamId = kafkaRequest.getTeamId();
            CreateTeamTaskDto dto = kafkaRequest.getCreateTeamTaskDto();

            teamTaskService.createTeamTask(userId, teamId, dto);
            sendSuccessMessage("team-create-task-response", requestId);
        } catch (Exception e) {
            sendErrorMessage("team-create-task-response", requestId, e.getMessage());
        }
    }

    @KafkaListener(topics = "team-get-tasks")
    public void getTeamTasks(String message) {
        String requestId = "";

        try {
            message = message.substring(1, message.length() - 1).replace("\\\"", "\"");
            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);
            requestId = kafkaRequest.getRequestId();
            Long teamId = kafkaRequest.getTeamId();

            Map<String, Object> response = new HashMap<>();
            response.put("requestId", requestId);
            response.put("response-message", "SUCCESS");
            List<TeamTask> teamTasks = teamTaskService.getAllTeamTasks(teamId);

            if (teamTasks == null || teamTasks.isEmpty()) {
                response.put("response-empty", "TRUE");
            } else {
                response.put("team-tasks", teamTasks);
            }

            // Используем JavaTimeModule чтобы правильно сериализовать datetime
            String jsonResponse = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(response);
            kafkaTemplate.send("team-get-tasks-response", jsonResponse);
        } catch (Exception e) {
            log.error("Ошибка при отправке задач: " + e.getMessage());
            sendErrorMessage("team-get-tasks-response", requestId, e.getMessage());
        }
    }

    @KafkaListener(topics = "team-edit-task")
    public void teamEditTask(String message) {
        String requestId = "";

        try {
            message = message.substring(1, message.length() - 1).replace("\\\"", "\"");
            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);
            requestId = kafkaRequest.getRequestId();

            TeamTask editedTask = kafkaRequest.getEditedTeamTask();
            teamTaskService.updateTeamTask(editedTask);
            sendSuccessMessage("team-edit-task-response", requestId);
        } catch (Exception e) {
            sendErrorMessage("team-edit-task-response", requestId, e.getMessage());
        }
    }

    // Listener для получения всех задач
    @KafkaListener(topics = "task-request", groupId = "task-group")
    @SneakyThrows
    public void handleTaskRequest(String message)  {
        String requestId = UUID.randomUUID().toString();

        try {
            // Очистка экранизации
            message = message.replace("\\\"", "\"");
            message = message.substring(1, message.length() - 1);

            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);
            String action = kafkaRequest.getAction();
            JsonNode jsonNode = objectMapper.readTree(message);
            JsonNode requestIdNode = jsonNode.get("requestId");
            if (requestIdNode != null) {
                requestId = requestIdNode.asText();
            }

            Long userId = jsonNode.get("userId") != null ? jsonNode.get("userId").asLong() : null;
            if ("GET_ALL_TASKS".equals(action)) {
                List<Task> userTasks = taskService.processGetAllTasks(userId);
                log.info("user tasks: " + userTasks);

                if (userTasks == null || userTasks.isEmpty()) {
                    Map<String, Object> emptyResponse = Map.of(
                            "requestId", requestId,
                            "response-status", "SUCCESS",
                            "tasks-empty", true
                    );
                    String emptyResponseJson = objectMapper.writeValueAsString(emptyResponse);

                    log.info("Отправлен ответ о пустых задачах: {}", emptyResponseJson);
                    kafkaTemplate.send("task-response", emptyResponseJson);
                } else {
                    String serializedTasks = TaskSerializer.serializeTaskList(userTasks);

                    Map<String, Object> response = Map.of(
                            "requestId", requestId,
                            "response-status", "SUCCESS",
                            "tasks", serializedTasks
                    );
                    String responseJson = objectMapper.writeValueAsString(response);

                    log.info("Отправлен ответ на task-response: {}", responseJson);
                    kafkaTemplate.send("task-response", responseJson);
                }
            }
        } catch (JsonProcessingException e) {
            Map<String, Object> errorResponse = Map.of(
                    "requestId", requestId,
                    "response-status", "ERROR",
                    "response-message", e.getMessage()
            );
            kafkaTemplate.send("task-response", objectMapper.writeValueAsString(errorResponse));
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                    "requestId", requestId,
                    "response-status", "ERROR",
                    "message", e.getMessage()
            );

            kafkaTemplate.send("task-response", objectMapper.writeValueAsString(errorResponse));
        }
    }

    @KafkaListener(topics = "team-delete-task")
    public void teamDeleteTask(String message) {
        String requestId = "";

        try {
            message = message.substring(1, message.length() - 1).replace("\\\"", "\"");
            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);
            requestId = kafkaRequest.getRequestId();
            Long teamId = kafkaRequest.getTeamId();
            Long taskId = kafkaRequest.getTaskId();

            teamTaskService.deleteTeamTask(teamId, taskId);
            sendSuccessMessage("team-delete-task-response", requestId);
        } catch (Exception e) {
            sendErrorMessage("team-delete-task-response", requestId, e.getMessage());
        }
    }

    @KafkaListener(topics = "team-add-subtask")
    public void teamAddSubtask(String message) {
        String requestId = "";

        try {
            message = message.substring(1, message.length() - 1).replace("\\\"", "\"");
            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);
            requestId = kafkaRequest.getRequestId();
            CreateSubtaskDto subtask = kafkaRequest.getSubtask();
            Long userId = kafkaRequest.getUserId();
            Long teamId = kafkaRequest.getTeamId();
            Long taskId = kafkaRequest.getTaskId();

            teamTaskService.addTeamSubTask(subtask, userId, teamId, taskId);
            sendSuccessMessage("team-add-subtask-response", requestId);
        } catch (Exception e) {
            sendErrorMessage("team-add-subtask-response", requestId, e.getMessage());
        }
    }

    @KafkaListener(topics = "team-delete-subtask")
    public void teamDeleteSubtask(String message) {
        String requestId = "";

        try {
            message = message.substring(1, message.length() - 1).replace("\\\"", "\"");
            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);
            requestId = kafkaRequest.getRequestId();
            Long subtaskId = kafkaRequest.getSubtaskId();
            Long userId = kafkaRequest.getUserId();
            Long teamId = kafkaRequest.getTeamId();
            Long taskId = kafkaRequest.getTaskId();

            teamTaskService.deleteTeamSubTask(subtaskId, userId, teamId, taskId);
            sendSuccessMessage("team-delete-subtask-response", requestId);
        } catch (Exception e) {
            sendErrorMessage("team-delete-subtask-response", requestId, e.getMessage());
        }
    }

    @KafkaListener(topics = "task-create", groupId = "task-group")
    public void handleTaskCreateRequest(String message) throws JsonProcessingException {
        String requestId = UUID.randomUUID().toString();

        try {
            message = message.substring(1, message.length() - 1);
            message = message.replace("\\\"", "\"");
            String jsonRequestTasks = message.replace("\\\\", "\\"); // v2

            KafkaRequest kafkaRequest = KafkaRequest.fromJson(jsonRequestTasks);
            log.info("TaskRequest на создание задачи: " + kafkaRequest.toString());
            Task task = Task.fromJson(kafkaRequest.getTask());
            log.info("Задача, извлеченная из запроса на создание: " + task.toString());
            Task taskToSave = new Task();

            requestId = kafkaRequest.getRequestId();

            // Маппинг данных
            taskToSave.setTitle(task.getTitle());
            taskToSave.setDescription(task.getDescription());
            taskToSave.setStatus(task.getStatus());
            taskToSave.setPriority(task.getPriority());
            taskToSave.setCreatorId(task.getCreatorId());
            taskToSave.setDueDate(task.getDueDate());
            taskToSave.setHours(task.getHours());
            taskToSave.setMinutes(task.getMinutes());
            taskToSave.setCreatedAt(task.getCreatedAt());

            taskRepository.save(taskToSave);
            sendSuccessMessage("task-create-response", requestId);
        } catch (Exception e) {
            log.error("Ошибка при попытке сохранить задачу: " + e.getMessage());

            Map<String, Object> errorResponse = Map.of(
                    "requestId", requestId,
                    "response-status", "ERROR",
                    "response-message", e.getMessage()
            );

            kafkaTemplate.send("task-create-response", objectMapper.writeValueAsString(errorResponse));
        }
    }

    @KafkaListener(topics = "team-update-subtask")
    public void updateSubtask(String message) {
        String requestId = "";

        try {
            message = message.substring(1, message.length() - 1).replace("\\\"", "\"");
            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);
            requestId = kafkaRequest.getRequestId();
            Long userId = kafkaRequest.getUserId();
            Long teamId = kafkaRequest.getTeamId();
            Long taskId = kafkaRequest.getTaskId();

            CreateSubtaskDto updatedSubtask = kafkaRequest.getSubtask();
            teamTaskService.editTeamSubtask(updatedSubtask, userId, teamId, taskId);
            sendSuccessMessage("team-update-subtask-response", requestId);
        } catch (Exception e) {
            sendErrorMessage("team-update-subtask-response", requestId, e.getMessage());
        }
    }

    @SneakyThrows
    private void sendErrorMessage(String topicName, String requestId, String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("requestId", requestId);
        response.put("response-message", "ERROR:" + errorMessage);

        kafkaTemplate.send(topicName, objectMapper.writeValueAsString(response));
    }

    @SneakyThrows
    private void sendSuccessMessage(String topicName, String requestId) {
        Map<String, Object> response = new HashMap<>();
        response.put("requestId", requestId);
        response.put("response-message", "SUCCESS");

        kafkaTemplate.send(topicName, objectMapper.writeValueAsString(response));
    }


}