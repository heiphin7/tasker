package com.FrontendService.service;

import com.FrontendService.json.TaskSerializer;
import com.FrontendService.json.UserSerializer;
import com.FrontendService.model.*;
import com.FrontendService.model.dto.CreateTeamDTO;
import com.FrontendService.model.dto.CreateTeamTaskDto;
import com.FrontendService.model.dto.TeamInfoDto;
import com.FrontendService.model.kafka.KafkaRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskKafkaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Мапа для хранения ожидающих ответов
    private final Map<String, List<Task>> getAllTasksResponseFuture = new ConcurrentHashMap<>();
    private final Map<String, String> updateTasksResponseFuture = new ConcurrentHashMap<>();
    private final Map<String, Object> responseFutures = new ConcurrentHashMap<>();




    @KafkaListener(topics = "task-response", groupId = "task-group")
    public void handleTaskResponse(String message) {
        try {
            KafkaRequest request = KafkaRequest.fromJson(message);

            // Payload from message:
            String requestId = request.getRequestId();
            String responseStatus = request.getResponseStatus();
            Boolean taskEmpty = request.getTaskEmpty();
            String serializedTasks = request.getTasks();

            if (responseStatus == null || "ERROR".equals(responseStatus)) {
                String errorMessage = request.getResponseMessage();
                log.error("Ошибка при попытке получения всех задач: {}", errorMessage);
                return;
            }

            log.info("Проверка флага tasks-empty: {}", taskEmpty);
            // Проверка на наличие флага tasks-empty
            if (taskEmpty != null && taskEmpty) {
                getAllTasksResponseFuture.put(requestId, new ArrayList<>()); // Ложим пустой список, так как задачи нет
                return;
            }

            List<Task> tasks = TaskSerializer.deserializeTaskList(serializedTasks);
            if (tasks == null) {
                tasks = new ArrayList<>();
            }

            getAllTasksResponseFuture.put(requestId, tasks);
        } catch (Exception e) {
            log.error("Ошибка в процессе обработки сообщения: {}", e.getMessage(), e);
        }
    }

    // Создание задачи через TaskService
    public void saveTask(Task newTask) throws IllegalArgumentException {
        try {
            // Проверка всех полей перед отправкой
            if (newTask.getTitle() == null || newTask.getTitle().isEmpty() ||
                    newTask.getDescription() == null || newTask.getDescription().isEmpty() ||
                    newTask.getPriority() == null ||
                    newTask.getDueDate() == null) {
                throw new IllegalArgumentException("Все поля должны быть заполнены!");
            }

            String serializedTask = TaskSerializer.serializeTask(newTask);
            String requestId = UUID.randomUUID().toString();
            Map<String, Object> request = new HashMap<>();
            request.put("requestId", requestId);
            request.put("userId", newTask.getCreatorId());
            request.put("tasks", serializedTask);

            String jsonRequest = objectMapper.writeValueAsString(request);
            log.info("Запрос на создание задачи: " + jsonRequest);
            kafkaTemplate.send("task-create", jsonRequest) .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("❌ Ошибка при отправке запроса: {}", ex.getMessage());
                } else {
                    log.info("✅ Запрос на создание задачи успешно отправлен");
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Ошибка при попытке сериализовать запрос: " + e.getMessage());
        }
    }




    // updateTask
    @SneakyThrows
    public String updateTask(Task task, Long taskToChange, Long userId) {
        String requestId = UUID.randomUUID().toString();

        String serializedTask = TaskSerializer.serializeTask(task);

        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("userId", userId);
        request.put("tasks", serializedTask);
        request.put("taskToChangeId", taskToChange);

        String jsonRequest = objectMapper.writeValueAsString(request);
        log.info("Запрос на изменение задачи: " + jsonRequest);

        kafkaTemplate.send("task-update", jsonRequest);

        // Ждём ответ с таймаутом через Thread.sleep()
        for (int i = 0; i < 20; i++) {
            Thread.sleep(100);
            if (updateTasksResponseFuture.containsKey(requestId)) {
                String response = updateTasksResponseFuture.remove(requestId);
                Map<String, String> responseMap = objectMapper.readValue(response, new TypeReference<>() {});

                String status = responseMap.get("status");
                if ("success".equals(status)) {
                    return "SUCCESS";
                } else {
                    String errorMessage = responseMap.getOrDefault("message", "Неизвестная ошибка");
                    return "ERROR:" + errorMessage;
                }
            }
        }

        log.error("⏰ Таймаут при ожидании ответа");
        return "ERROR:Таймаут при ожидании ответа";
    }


    @KafkaListener(topics = "task-update-response", groupId = "task-group")
    public void handleTaskUpdateResponse(String message) {
        log.info("Ответ на обновление задачи: " + message);

        try {
            Map<String, String> response = objectMapper.readValue(message, new TypeReference<>() {});
            String requestId = response.get("requestId");

            if (requestId != null) {
                updateTasksResponseFuture.put(requestId, message);
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке ответа на обновление задачи", e);
        }
    }

    // Удаление задачи
    @SneakyThrows
    public String deleteTask(Long userId, Long taskId) {
        String requestId = UUID.randomUUID().toString();

        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("userId", userId);
        request.put("taskId", taskId);

        kafkaTemplate.send("task-delete", objectMapper.writeValueAsString(request));

        for (int i = 0; i < 30; i++) {
            if (responseFutures.containsKey(requestId)) {
                String response = (String) responseFutures.get(requestId);
                responseFutures.remove(requestId);
                return response;
            }

            Thread.sleep(100);
        }

        log.error("Превышено время ожидания для удаления задачи");
        return "ERROR:Ошибка при удалении задачи";
    }

    @KafkaListener(topics = "task-delete-response", groupId = "task-group")
    public void handleTaskDeleteResponse(String message) {
        String requestId = "";

        try {
            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);
            requestId = kafkaRequest.getRequestId();
            String responseMessage = kafkaRequest.getResponseMessage();

            responseFutures.put(requestId, responseMessage);
        } catch (Exception e) {
            log.error("Ошибка при удалении задачи: " + e.getMessage());
            responseFutures.put(requestId, "ERROR:" + e.getMessage());
        }
    }


    // Получение всех команд пользователя
    @SneakyThrows
    public List<Team> getAllTeamsByUser(Long userId) {
        String requestId = UUID.randomUUID().toString();

        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("userId", userId);

        String jsonRequest = objectMapper.writeValueAsString(request);
        kafkaTemplate.send("user-get-teams", jsonRequest);

        for (int i = 0; i < 50; i++) {
            if (responseFutures.containsKey(requestId)) {
                List<Team> userTeams = (List<Team>) responseFutures.get(requestId);
                responseFutures.remove(requestId);
                return userTeams;
            }

            Thread.sleep(100);
        }

        log.error("Ошибка при ожидании команд пользователя");
        return new ArrayList<>();
    }

    @KafkaListener(topics = "user-get-teams-response", groupId = "user-group")
    public void handleUserTeamsResponse(String message) {
        log.info("Пришел ответ на usr-get-teams-response: " + message);
        String requestId = null;

        try {
            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);
            requestId = kafkaRequest.getRequestId();
            String requestStatus = kafkaRequest.getResponseStatus();

            if (requestStatus.equals("SUCCESS")) {
                if (kafkaRequest.getResponseEmpty()!= null) {
                    responseFutures.put(requestId, new ArrayList<>());
                } else {
                    List<Team> userTeams = UserSerializer.deserializeTeamList
                            (kafkaRequest.getSerializedTeamList());

                    log.info("Десериализованные задачи пользователя: " + userTeams);
                    responseFutures.put(requestId, userTeams);
                }
            } else {
                log.info("Ошибка при получении списка задач: " + kafkaRequest.getResponseMessage());
                responseFutures.put(requestId, new ArrayList<>());
            }
        } catch (Exception e) {
            log.error("Ошибка при получении списка команд пользователя: " + e); // todo Убрать потом
            responseFutures.put(requestId, new ArrayList<>());
        }
    }

    @SneakyThrows
    public String createTeam(CreateTeamDTO createTeamDTO, Long userId) {
        String requestId = UUID.randomUUID().toString();

        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("userId", userId);
        request.put("createTaskDto", objectMapper.writeValueAsString(createTeamDTO));

        String jsonRequest = objectMapper.writeValueAsString(request);
        kafkaTemplate.send("user-create-team", jsonRequest);

        for (int i = 0; i < 30; i++) {
            if (responseFutures.containsKey(requestId)) {
                String response = (String) responseFutures.get(requestId);
                responseFutures.remove(requestId);
                return response;
            }

            Thread.sleep(100);
        }

        log.error("Превышено время ожидания для ответа по созданию команды");
        return "ERROR:TIMEOUT-EXCEPTION";
    }

    @KafkaListener(topics = "user-create-team-response", groupId = "user-group")
    public void handleCreateTeamResponse(String message) {
        log.info("Пришет ответ на topic: user-create-team: " + message);

        try {
            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);

            String requestId = kafkaRequest.getRequestId();
            String responseStatus = kafkaRequest.getResponseStatus();
            String responseMessage = "";

            if (!responseStatus.equals("SUCCESS")) {
                responseMessage = "ERROR:" + kafkaRequest.getResponseMessage();
            }

            responseFutures.put(requestId, responseMessage);
        } catch (Exception e) {
            log.error("Ошибка при получении ответа о создании команды: " + e);
        }
    }

    // Получение всех задач пользователя
    public List<Task> getAllTasksByUserId(Long userId) {
        String requestId = UUID.randomUUID().toString();
        Map<String, Object> request = new HashMap<>();
        request.put("action", "GET_ALL_TASKS");
        request.put("requestId", requestId);
        request.put("userId", userId);

        try {
            String jsonRequest = objectMapper.writeValueAsString(request);
            kafkaTemplate.send("task-request", jsonRequest);

            List<Task> tasks = null;
            for (int i = 0; i < 30; i++) {
                tasks = getAllTasksResponseFuture.get(requestId);
                if (tasks != null) {
                    log.info("✅ Ответ получен, список задач: {}", tasks);
                    getAllTasksResponseFuture.remove(requestId);
                    return tasks;
                }
                Thread.sleep(100);
            }

            return new ArrayList<>();

        } catch (JsonProcessingException | InterruptedException e) {
            return new ArrayList<>();
        }
    }
    
    // Получение информации о команде
    @SneakyThrows
    public TeamInfoDto getTeamInfo(Long userId, Long teamId) throws IllegalArgumentException {
        String requestId = UUID.randomUUID().toString();
        
        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("userId", userId);
        request.put("teamId", teamId);
        kafkaTemplate.send("team-get-info", objectMapper.writeValueAsString(request));


        for (int i = 0; i < 30; i++) {
            if (responseFutures.containsKey(requestId)) {
                Object response = responseFutures.get(requestId);
                if (response instanceof TeamInfoDto) {
                    TeamInfoDto teamInfoDto = (TeamInfoDto) responseFutures.get(requestId);
                    responseFutures.remove(requestId);
                    return teamInfoDto;
                } else {
                    responseFutures.remove(requestId);
                    return null;
                }
            }

            Thread.sleep(100);
        }

        return null;
    }
    
    @KafkaListener(topics = "team-get-info-response", groupId = "team-group")
    public void handleTeamInfoResponse(String message) {
        try {
            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);

            String requestId = kafkaRequest.getRequestId();
            String responseStatus = kafkaRequest.getResponseStatus();
            
            if (responseStatus.equals("SUCCESS")) {
                Team team = Team.fromJson(kafkaRequest.getTeamInfo());
                List<User> members = objectMapper.readValue(
                        kafkaRequest.getMembers(), objectMapper.getTypeFactory().constructCollectionType(List.class, User.class)
                );

                TeamInfoDto teamInfoDto = new TeamInfoDto(team, team.getIsCorporate(), members);
                responseFutures.put(requestId, teamInfoDto);
            } else {
                responseFutures.put(requestId, "ERROR");
            }

        } catch (Exception e) {
            log.error("Ошибка при получении информации о команде: " + e);
        }
    }

    // Удаление пользователя
    @SneakyThrows
    public String deleteMember(Long memberId, Long teamId, Long userId) {
        String requestId = UUID.randomUUID().toString();

        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("memberId", memberId);
        request.put("userId", userId);
        request.put("teamId", teamId);

        kafkaTemplate.send("team-delete-member", objectMapper.writeValueAsString(request));

        // Обрабатывает ответ, положенный в responseFutures
        for (int i = 0; i < 50; i++) {
            if (responseFutures.containsKey(requestId)) {
                String responseMessage = (String) responseFutures.get(requestId);
                log.info("Найден ответ на team-delete-member " + responseMessage);
                responseFutures.remove(requestId);
                return responseMessage;
            }

            Thread.sleep(100);
        }

        return "Ошибка при попытке удалить пользователя";
    }

    @KafkaListener(topics = "team-delete-member-response", groupId = "team-group")
    public void handleDeleteMemberResponse(String message) {
        String requestId = "";
        try {
            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);
            requestId = kafkaRequest.getRequestId();
            responseFutures.put(requestId, kafkaRequest.getResponseMessage());
        } catch (Exception e) {
            log.error("Ошибка при удалении пользователя: " + e.getMessage());
        }
    }

    // Добавление пользователя
    @SneakyThrows
    public String addMembers(String username, Long teamId) {
        String requestId = UUID.randomUUID().toString();

        Map<String, Object> request = new HashMap<>();
        request.put("username", username);
        request.put("requestId" , requestId);
        request.put("teamId", teamId);
        kafkaTemplate.send("team-add-member", objectMapper.writeValueAsString(request));

        for (int i = 0; i < 50; i++) {
            if (responseFutures.containsKey(requestId)) {
                String responseMessage = (String) responseFutures.get(requestId);
                responseFutures.remove(requestId);
                return responseMessage;
            }

            Thread.sleep(100);
        }

        return "Ошибка при попытке добавить пользователя в группу";
    }

    @KafkaListener(topics = "team-add-member-response", groupId = "team-group")
    public void handleAddMembersResponse(String message) {
        String requestId = "";

        try {
            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);

            requestId = kafkaRequest.getRequestId();
            String responseMessage = kafkaRequest.getResponseMessage();
            responseFutures.put(requestId, responseMessage);
        } catch (Exception e) {
            log.error("Ошибка при попытке обработать ответ на team-add-member-response: " + e);
            responseFutures.put(requestId, e.getMessage());
        }
    }

    // Выход из команды
    @SneakyThrows
    public String quitFromTeam(Long userId, Long teamId) {
        String requestId = UUID.randomUUID().toString();

        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("userId", userId);
        request.put("teamId", teamId);

        kafkaTemplate.send("team-quit-member", objectMapper.writeValueAsString(request));

        return waitForResponse(requestId, "Ошибка при попытке выйти из команды");
    }

    // Покинуть команду как админ
    @SneakyThrows
    public String leaveTeamAsAdmin(Long userId, Long teamId, Long nextAdmin) {
        String requestId = UUID.randomUUID().toString();
        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("userId", userId);
        request.put("teamId", teamId);

        if (nextAdmin != null) {
            request.put("nextAdmin", nextAdmin);  // Передача прав новому админу
        }

        kafkaTemplate.send("team-leave-admin", objectMapper.writeValueAsString(request));
        return waitForResponse(requestId, "Ошибка при попытке покинуть команду как админ");
    }

    private String waitForResponse(String requestId, String errorMessage) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            if (responseFutures.containsKey(requestId)) {
                String responseMessage = (String) responseFutures.get(requestId);
                responseFutures.remove(requestId);
                return responseMessage;
            }
            Thread.sleep(100);
        }
        return errorMessage;
    }

    @KafkaListener(topics = {"team-quit-member-response", "team-leave-admin-response"}, groupId = "team-group")
    public void handleResponse(String message) {
        try {
            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);
            responseFutures.put(kafkaRequest.getRequestId(), kafkaRequest.getResponseMessage());
        } catch (Exception e) {
            log.error("Ошибка при обработке ответа: " + e.getMessage());
        }
    }

    // Создание командной задачи
    @SneakyThrows
    public String createTeamTask(Long userId, Long teamId, CreateTeamTaskDto dto) {
        String firstRequestId = UUID.randomUUID().toString();

        // Проверка существования команды и принадлежности юзера
        Map<String, Object> userAndTeamCheckRequest = new HashMap<>();
        userAndTeamCheckRequest.put("requestId", firstRequestId);
        userAndTeamCheckRequest.put("userId", userId);
        userAndTeamCheckRequest.put("teamId", teamId);

        kafkaTemplate.send("team-check-user-team", objectMapper.writeValueAsString(userAndTeamCheckRequest));

        // Ждём ответ
        for (int i = 0; i < 20; i++) {
            Thread.sleep(100);
            if (responseFutures.containsKey(firstRequestId)) {
                String checkResponse = (String) responseFutures.remove(firstRequestId);
                if (checkResponse.startsWith("ERROR")) {
                    return checkResponse;
                }

                break;
            }
        }

        // Отправляем запрос на создание задачи
        String secondRequestId = UUID.randomUUID().toString();

        Map<String, Object> taskRequest = new HashMap<>();
        taskRequest.put("requestId", secondRequestId);
        taskRequest.put("userId", userId);
        taskRequest.put("teamId", teamId);
        taskRequest.put("dto", dto);

        kafkaTemplate.send("team-create-task", objectMapper.writeValueAsString(taskRequest));

        // Ждём ответ от TaskService
        for (int i = 0; i < 80; i++) {
            Thread.sleep(100);
            if (responseFutures.containsKey(secondRequestId)) {
                return (String) responseFutures.remove(secondRequestId);
            }
        }

        log.error("Ошибка при ожидании ответа от team-create-task");
        return "ERROR:Ошибка при ожидании ответа";
    }

    @KafkaListener(topics = "team-check-user-team-response", groupId = "user-group")
    public void handleCheckUserAndTeamResponse(String message) {
        String requestId = "";

        try {
            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);
            requestId = kafkaRequest.getRequestId();
            responseFutures.put(requestId, kafkaRequest.getResponseMessage());
        } catch (Exception e) {
            responseFutures.put(requestId, "ERROR:" + e.getMessage());
        }
    }

    @KafkaListener(topics = "team-create-task-response", groupId = "team-group")
    public void handleCreateTeamTaskResponse(String message) {
        try {
            Map<String, String> response = objectMapper.readValue(message, new TypeReference<>() {});
            String requestId = response.get("requestId");

            if (requestId != null) {
                responseFutures.put(requestId, response.get("response-message"));
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке ответа на создание задачи", e);
        }
    }


    // Получение всех командных задач команды
    public List<TeamTask> getTeamTasks(Long teamId) throws Exception {
        String requestId = UUID.randomUUID().toString();
        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("teamId", teamId);
        kafkaTemplate.send("team-get-tasks", objectMapper.writeValueAsString(request));

        for (int i = 0; i < 100; i++) {
            Thread.sleep(100);

            if (responseFutures.containsKey(requestId)) {
                return (List<TeamTask>) responseFutures.remove(requestId);
            }
        }

        log.error("Ошибка при получении задач команды");
        return new ArrayList<>();
    }


    @KafkaListener(topics = "team-get-tasks-response")
    public void handleGetTeamTasksResponse(String message) {
        String requestId = "";

        try {
            ObjectMapper privateObjectMapper = new ObjectMapper();
            privateObjectMapper.registerModule(new JavaTimeModule());

            KafkaRequest kafkaRequest = privateObjectMapper.readValue(message, KafkaRequest.class);
            requestId = kafkaRequest.getRequestId();
            String responseMessage = kafkaRequest.getResponseMessage();

            if (responseMessage.startsWith("SUCCESS")) {
                if (kafkaRequest.getResponseEmpty() != null && kafkaRequest.getResponseEmpty().equals("TRUE")) {
                    responseFutures.put(requestId, new ArrayList<>());
                    return;
                }
            }

            responseFutures.put(requestId, kafkaRequest.getTeamTasks());
        } catch (Exception e) {
            log.error("Ошибка при обработке ответа на team-get-tasks: " + e.getMessage() + " " + e); // todo remove
            responseFutures.put(requestId, "ERROR: " + e.getMessage());
        }
    }

    @SneakyThrows
    public String editTeamTask(TeamTask editedTask, Long teamId) {
        String requestID = UUID.randomUUID().toString();
        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestID);
        request.put("editedTask", editedTask);
        request.put("teamId", teamId);

        kafkaTemplate.send("team-edit-task", objectMapper.writeValueAsString(request));

        // Ожидание ответа
        for (int i = 0; i < 50; i++) {
            Thread.sleep(100);

            if (responseFutures.containsKey(requestID)) {
                return (String) responseFutures.get(requestID);
            }
        }

        log.error("Ошибка при ожидании ответа на team-edit-task-response");
        return "ERROR:Ошибка при ожидании ответа";
    }

    @KafkaListener(topics = "team-edit-task-response")
    public void handleTeamEditTaskResponse(String message) {
        String requestId = "";

        try {
            ObjectMapper privateObjectMapper = new ObjectMapper();
            privateObjectMapper.registerModule(new JavaTimeModule());

            KafkaRequest kafkaRequest = privateObjectMapper.readValue(message, KafkaRequest.class);
            requestId = kafkaRequest.getRequestId();
            String responseStatus = kafkaRequest.getResponseStatus();

            if (kafkaRequest.getResponseMessage() != null && kafkaRequest.getResponseMessage().startsWith("ERROR")) {
                responseFutures.put(requestId, kafkaRequest.getResponseMessage());
            }

            responseFutures.put(requestId, responseStatus);

        } catch (Exception e) {
            responseFutures.put(requestId, "ERROR" + e.getMessage());
        }
    }

    @SneakyThrows
    public String deleteTeamTask(Long taskId, Long userId, Long teamId) {
        String requestId = UUID.randomUUID().toString();
        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("taskId", taskId);
        request.put("teamId", teamId);
        request.put("userId", userId);

        kafkaTemplate.send("team-delete-task", objectMapper.writeValueAsString(request));

        for (int i = 0; i < 50; i++) {
            Thread.sleep(100);

            if (responseFutures.containsKey(requestId)) {
                return (String) responseFutures.get(requestId);
            }
        }

        log.error("Ошибка при ожидании ответа на team-delete-task-response");
        return "ERROR:Ошибка при ожидании ответа";
    }

    @KafkaListener(topics = "team-delete-task-response")
    public void handleTeamDeleteTaskResponse(String message) {
        String requestId = "";

        try {
            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);
            requestId = kafkaRequest.getRequestId();

            if (kafkaRequest.getResponseMessage() != null) {
                responseFutures.put(requestId, kafkaRequest.getResponseMessage());
                return;
            }

            responseFutures.put(requestId, kafkaRequest.getResponseStatus());
        } catch (Exception e) {
            log.error("Ошибка при обработке ответа на team-delete-task-response: " + e.getMessage());
            responseFutures.put(requestId, "ERROR: " + e.getMessage());
        }
    }

    @SneakyThrows
    public String addSubtask(Long teamId, Long userId, Subtask subTask, Long taskId) {
        String requestId = UUID.randomUUID().toString();

        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("userId", userId);
        request.put("subTask", subTask);
        request.put("teamId", teamId);
        request.put("taskId", taskId); // ID Родительской задачи

        kafkaTemplate.send("team-add-subtask", objectMapper.writeValueAsString(request));

        for (int i = 0; i < 50; i++) {
            Thread.sleep(100);

            if (responseFutures.containsKey(requestId)) {
                return (String) responseFutures.get(requestId);
            }
        }

        return "ERROR:Ошибка при ожидании ответа";
    }

    @KafkaListener(topics = "team-add-subtask-response")
    public void handleAddSubtaskResponse(String message) {
        String requestId = "";

        try {
            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);
            requestId = kafkaRequest.getRequestId();

            if (kafkaRequest.getResponseMessage() != null) {
                responseFutures.put(requestId, kafkaRequest.getResponseMessage());
                return;
            }

            responseFutures.put(requestId, kafkaRequest.getResponseStatus());
        } catch (Exception e) {
            responseFutures.put(requestId, "ERROR:" + e.getMessage());
        }
    }


    @SneakyThrows
    public String deleteSubtask(Long subtaskId, Long teamId, Long taskId) {
        String requestId = UUID.randomUUID().toString();

        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("teamId", teamId);
        request.put("subtaskId", subtaskId);
        request.put("taskId", taskId);

        kafkaTemplate.send("team-delete-subtask", objectMapper.writeValueAsString(request));

        for (int i = 0; i < 50; i++) {
            Thread.sleep(100);

            if (responseFutures.containsKey(requestId)) {
                return (String) responseFutures.get(requestId);
            }
        }

        return "ERROR:Ошибка при ожидании ответа";
    }

    @KafkaListener(topics = "team-delete-subtask-response")
    public void handleDeleteSubtaskResponse(String message) {
        String requestId = "";

        try {
            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);
            requestId = kafkaRequest.getRequestId();

            if (kafkaRequest.getResponseMessage() != null) {
                responseFutures.put(requestId, kafkaRequest.getResponseMessage());
                return;
            }

            responseFutures.put(requestId, kafkaRequest.getResponseStatus());
        } catch (Exception e) {
            responseFutures.put(requestId, "ERROR:" + e.getMessage());
        }
    }

    @SneakyThrows
    public String updateSubtask(Long teamId, Long taskId, Subtask updatedSubtask, Long userId) {
        String requestId = UUID.randomUUID().toString();

        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("teamId", teamId);
        request.put("taskId", taskId);
        request.put("userId", userId);
        request.put("subTask", updatedSubtask);

        kafkaTemplate.send("team-update-subtask", objectMapper.writeValueAsString(request));

        for (int i = 0; i < 50; i++) {
            Thread.sleep(100);

            if (responseFutures.containsKey(requestId)) {
                return (String) responseFutures.get(requestId);
            }
        }

        return "ERROR:Ошибка при ожидании ответа";
    }

    @KafkaListener(topics = "team-update-subtask-response")
    public void handleUpdateSubtaskResponse(String message) {
        String requestId = "";

        try {
            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);
            requestId = kafkaRequest.getRequestId();

            if (kafkaRequest.getResponseMessage() != null) {
                responseFutures.put(requestId, kafkaRequest.getResponseMessage());
                return;
            }

            responseFutures.put(requestId, kafkaRequest.getResponseStatus());
        } catch (Exception e) {
            responseFutures.put(requestId, "ERROR:" + e.getMessage());
        }
    }

    @SneakyThrows
    public String reAssignAdmin(Long userId, Long newAdminId, Long teamId) {
        String requestId = UUID.randomUUID().toString();

        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("userId", userId);
        request.put("nextAdmin", newAdminId);
        request.put("teamId", teamId);

        kafkaTemplate.send("team-reassign-admin", objectMapper.writeValueAsString(request));

        for (int i = 0; i < 50; i++) {
            Thread.sleep(100);

            if (responseFutures.containsKey(requestId)) {
                return (String) responseFutures.get(requestId);
            }
        }

        return "ERROR:Ошибка при ожидании ответа";
    }

    @KafkaListener(topics = "team-reassign-admin-response")
    public void handleReassignAdminResponse(String message) {
        String requestId = "";

        try {
            KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);
            requestId = kafkaRequest.getRequestId();

            if (kafkaRequest.getResponseMessage() != null) {
                responseFutures.put(requestId, kafkaRequest.getResponseMessage());
                return;
            }

            responseFutures.put(requestId, kafkaRequest.getResponseStatus());
        } catch (Exception e) {
            responseFutures.put(requestId, "ERROR:" + e.getMessage());
        }
    }

    @SneakyThrows
    public Integer getUserNotifications(Long userId) {
        String requestId = UUID.randomUUID().toString();
        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("userId", userId);
        kafkaTemplate.send("user-get-notifications", objectMapper.writeValueAsString(request));

        for (int i = 0; i < 50; i++) {
            Thread.sleep(100);


            if (responseFutures.containsKey(requestId)) {
                return Integer.parseInt((String) responseFutures.get(requestId));
            }
        }

        return 0;
    }

    @KafkaListener(topics = "user-get-notifications-response")
    public void handleUserGetNotificationsResponse(String message) {
        String requestId = "";

        KafkaRequest kafkaRequest = KafkaRequest.fromJson(message);
        requestId = kafkaRequest.getRequestId();

        if (kafkaRequest.getResponseMessage() != null) {
            responseFutures.put(requestId, kafkaRequest.getResponseMessage());
            return;
        }

        responseFutures.put(requestId, kafkaRequest.getResponseStatus());
    }

    @SneakyThrows
    public void resetNotificationCount(Long userId) {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);
        kafkaTemplate.send("user-reset-notifications", objectMapper.writeValueAsString(request));
    }
}