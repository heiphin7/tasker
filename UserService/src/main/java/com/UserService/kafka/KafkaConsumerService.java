package com.UserService.kafka;

import com.UserService.dto.CreateTeamDTO;
import com.UserService.model.Team;
import com.UserService.model.User;
import com.UserService.model.kafka.KafkaRequest;
import com.UserService.service.TeamService;
import com.UserService.service.UserSerializer;
import com.UserService.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final UserService userService;
    private final TeamService teamService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "user-get-teams", groupId = "user-group")
    @Transactional
    public void getAllUserTeams(String message) throws JsonProcessingException {
        Map<String, Object> response = new HashMap<>();
        String requestId = null;

        message = message.replace("\\\"", "\"");
        message = message.substring(1, message.length() - 1);

        KafkaRequest taskRequest = KafkaRequest.fromJson(message);
        requestId = taskRequest.getRequestId();

        User user = userService.findById(taskRequest.getUserId());

        try {
            if (user == null) {
                response.put("requestId", requestId);
                response.put("response-status", "ERROR");
                response.put("response-message", "User not found!");
            } else {
                List<Team> userTeams = new CopyOnWriteArrayList<>(user.getTeams());

                response.put("requestId", requestId);
                response.put("response-status", "SUCCESS");

                if (!userTeams.isEmpty()) {
                    String serializedTeamList = UserSerializer.serializeTeamList(userTeams);
                    response.put("serializedTeamList", serializedTeamList);
                } else {
                    response.put("response-empty", "TRUE");
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при попытке вернуть все задачи пользователя: " + e);
            response.put("requestId", requestId);
            response.put("response-status", "ERROR");
            response.put("response-message", e.getMessage());
        }

        String jsonRequest = objectMapper.writeValueAsString(response);
        kafkaTemplate.send("user-get-teams-response", jsonRequest);
    }



    @KafkaListener(topics = "user-create-team", groupId = "user-group")
    public void createTeam(String message) throws JsonProcessingException {
        log.info("Запрос на создание команды: " + message);
        String requestId = "";
        Map<String, Object> response = new HashMap<>();

        try {
            message = message.substring(1, message.length() - 1);
            message = message.replace("\\\"", "\"");
            String jsonRequestTasks = message.replace("\\\\", "\\");

            KafkaRequest taskRequest = KafkaRequest.fromJson(jsonRequestTasks);

            CreateTeamDTO dto = CreateTeamDTO.fromJson(taskRequest.getCreateTaskDto());
            Long userId = taskRequest.getUserId();
            requestId = taskRequest.getRequestId();

            response.put("requestId", requestId);
            userService.createTeam(dto, userId);
            response.put("response-status", "SUCCESS");
        } catch (IllegalArgumentException | NullPointerException e) {
            response.put("response-status", "ERROR");
            response.put("response-message", e.getMessage());
            log.error("Ошибка при создании командны: " + e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при создании командны: " + e.getMessage());
        }

        kafkaTemplate.send("user-create-team-response", objectMapper.writeValueAsString(response));
    }

    @KafkaListener(topics = "team-get-info", groupId = "team-group")
    @Transactional
    public void getTeamInfo(String message) throws JsonProcessingException {
        String requestId = "";
        Map<String, Object> response = new HashMap<>();

        try {
            message = message.substring(1, message.length()- 1);
            message = message.replace("\\\"", "\"");

            KafkaRequest request = KafkaRequest.fromJson(message);
            requestId = request.getRequestId();
            Long userId = request.getUserId();
            Long teamId = request.getTeamId();

            response.put("requestId", requestId);
            Team team = teamService.findById(teamId);
            User user = userService.findById(userId);
            List<User> members = teamService.getMembersById(teamId);

            if (members == null) {
                response.put("response-status", "ERROR");
                response.put("response-message", "В команде нет участников!");
                kafkaTemplate.send("team-get-info-response", objectMapper.writeValueAsString(response));
                return;
            }

            if (team == null) {
                response.put("response-status", "ERROR");
                response.put("response-message", "Такой команды не существует");
                kafkaTemplate.send("team-get-info-response", objectMapper.writeValueAsString(response));
                return;
            }

            if (user == null) {
                response.put("response-status", "ERROR");
                response.put("user-empty", "TRUE");
                kafkaTemplate.send("team-get-info-response", objectMapper.writeValueAsString(response));
                return;
            }

            if (!members.contains(user)) {
                response.put("response-status", "ERROR");
                response.put("response-message", "Вы не состоите в данной команде!");
                kafkaTemplate.send("team-get-info-response", objectMapper.writeValueAsString(response));
                return;
            }

            response.put("response-status", "SUCCESS");
            response.put("teamInfo", objectMapper.writeValueAsString(team));
            response.put("team-members", objectMapper.writeValueAsString(members));
            kafkaTemplate.send("team-get-info-response", objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            response.put("requestId", requestId);
            response.put("response-status", "ERROR");
            response.put("response-message", e.getMessage());
            kafkaTemplate.send("team-get-info-response", objectMapper.writeValueAsString(response));
        }
    }


    @KafkaListener(topics = "team-delete-member", groupId = "team-group")
    public void deleteMember(String message) throws JsonProcessingException {
        String requestId = "";
        Map<String, Object> response = new HashMap<>();

        try {
            message = message.substring(1, message.length() - 1);
            message = message.replace("\\\"", "\"");

            KafkaRequest taskRequest = KafkaRequest.fromJson(message);
            requestId = taskRequest.getRequestId();

            teamService.deleteMemberFromTeam(taskRequest.getMemberId(), taskRequest.getTeamId(), taskRequest.getUserId());
            handleSuccessResponse("team-delete-member-response", requestId);
        } catch (Exception e) {
            response.put("requestId", requestId);
            response.put("response-message", "ERROR:" + e.getMessage());
            kafkaTemplate.send("team-delete-member-response", objectMapper.writeValueAsString(response));
        }
    }

    @KafkaListener(topics = "team-add-member", groupId = "team-group")
    public void addMember(String message) throws JsonProcessingException {
        String requestId = "";
        Map<String, Object> response = new HashMap<>();

        try {
            message = message.substring(1, message.length() - 1);
            message = message.replace("\\\"", "\"");

            KafkaRequest taskRequest = KafkaRequest.fromJson(message);
            requestId = taskRequest.getRequestId();

            teamService.addMemberToTeam(taskRequest.getUsername(), taskRequest.getTeamId());
            handleSuccessResponse("team-add-member-response", requestId);
        } catch (Exception e) {
            response.put("requestId", requestId);
            response.put("response-message", "ERROR:" + e.getMessage());
            kafkaTemplate.send("team-add-member-response", objectMapper.writeValueAsString(response));
        }
    }


    @KafkaListener(topics = "team-quit-member", groupId = "team-group")
    public void quitFromTeam(String message) throws JsonProcessingException {
        handleKafkaRequest(message, (taskRequest) -> {
            teamService.quitFromTeam(taskRequest.getUserId(), taskRequest.getTeamId());
            return "Вы успешно покинули команду!";
        }, "team-quit-member-response");
    }

    @KafkaListener(topics = "team-leave-admin", groupId = "team-group")
    public void leaveTeamAsAdmin(String message) throws JsonProcessingException {
        handleKafkaRequest(message, (taskRequest) -> {
            teamService.leaveTeamAsAdmin(taskRequest.getUserId(), taskRequest.getTeamId(), taskRequest.getNextAdmin());
            return "Вы успешно покинули команду как админ!";
        }, "team-leave-admin-response");
    }

    private void handleKafkaRequest(String message, Function<KafkaRequest, String> handler, String responseTopic) throws JsonProcessingException {
        String requestId = "";
        Map<String, Object> response = new HashMap<>();
        try {
            message = message.substring(1, message.length() - 1).replace("\\\"", "\"");
            KafkaRequest taskRequest = KafkaRequest.fromJson(message);
            requestId = taskRequest.getRequestId();

            String resultMessage = handler.apply(taskRequest);

            response.put("requestId", requestId);
            response.put("response-message", resultMessage);
            kafkaTemplate.send(responseTopic, objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            response.put("requestId", requestId);
            response.put("response-message", "ERROR:" + e.getMessage());
            kafkaTemplate.send(responseTopic, objectMapper.writeValueAsString(response));
        }
    }

    @KafkaListener(topics = "team-check-user-team")
    public void handleUserAndTeamCheck(String message) {
        String requestId = "";

        try {
            message = message.substring(1, message.length() - 1).replace("\\\"", "\"");
            KafkaRequest taskRequest = KafkaRequest.fromJson(message);
            requestId = taskRequest.getRequestId();
            Long userId = taskRequest.getUserId();
            Long teamId = taskRequest.getTeamId();

            userService.checkUserAndTeam(userId, teamId);
            handleSuccessResponse("team-check-user-team-response", requestId);
        } catch (Exception e) {
            handleErrorResponse("team-check-user-team-response", requestId, e.getMessage());
        }
    }

    @KafkaListener(topics = "team-reassign-admin")
    public void reAssignAdmin(String message) {
        String requestId = "";

        try {
            message = message.substring(1, message.length() - 1).replace("\\\"", "\"");
            KafkaRequest taskRequest = KafkaRequest.fromJson(message);
            requestId = taskRequest.getRequestId();
            Long nextAdmin = taskRequest.getNextAdmin();
            Long userId = taskRequest.getUserId();
            Long teamId = taskRequest.getTeamId();

            userService.reAssignAdmin(userId, nextAdmin, teamId);
            handleSuccessResponse("team-reassign-admin-response", requestId);
        } catch (Exception e) {
            handleErrorResponse("team-reassign-admin-response", requestId, e.getMessage());
        }
    }

    @KafkaListener(topics = "user-reset-notifications")
    public void resetUserNotifications(String message) {
        log.info("Check: " + message);
        message = message.substring(1, message.length() - 1).replace("\\\"", "\"");
        log.info("Check afteer: " + message);
        KafkaRequest request = KafkaRequest.fromJson(message);
        Long userId = request.getUserId();
        userService.resetNotifications(userId);
    }

    @KafkaListener(topics = "user-get-notifications")
    @SneakyThrows
    public void getUserNotifications(String message) {
        String requestId = "";
        message = message.substring(1, message.length() - 1).replace("\\\"", "\"");
        KafkaRequest taskRequest = KafkaRequest.fromJson(message);
        requestId = taskRequest.getRequestId();
        Long userId = taskRequest.getUserId();
        Integer notificationsCount = userService.getUserNotifications(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("requestId", requestId);
        response.put("response-message", notificationsCount);
        kafkaTemplate.send("user-get-notifications-response", objectMapper.writeValueAsString(response));
    }

    @SneakyThrows
    private void handleSuccessResponse(String topicName, String requestId) {
        Map<String, Object> response = new HashMap<>();
        response.put("requestId", requestId);
        response.put("response-message", "SUCCESS");

        kafkaTemplate.send(topicName, objectMapper.writeValueAsString(response));
    }

    @SneakyThrows
    private void handleErrorResponse(String topicName, String requestId, String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("requestId", requestId);
        response.put("response-message", "ERROR:" + errorMessage);

        kafkaTemplate.send(topicName, objectMapper.writeValueAsString(response));
    }
}
