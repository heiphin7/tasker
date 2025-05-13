package com.FrontendService.json;

import com.FrontendService.model.Team;
import com.FrontendService.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserSerializer {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // User -> String
    public static String serializeUser(User user) {
        try {
            return objectMapper.writeValueAsString(user);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("");
        }
    }

    // String -> User
    public static User deserializeUser(String userString) {
        try {
            return objectMapper.readValue(userString, User.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("");
        }
    }

    // List<User> -> String
    public static String serializeUserList(List<User> userList) {
        try {
            return objectMapper.writeValueAsString(userList);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("");
        }
    }

    // String -> List<User>
    public static List<User> deserializeUserList(String taskListString) {
        try {
            return objectMapper.readValue(taskListString,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, User.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("");
        }
    }

    // Team -> String
    public static String serializeTeam(Team team) {
        try {
            return objectMapper.writeValueAsString(team);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("");
        }
    }

    // String -> Team
    public static Team deserializeTeam(String teamString) {
        try {
            return objectMapper.readValue(teamString, Team.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("");
        }
    }

    // List<Team> -> String
    public static String serializeTeamList(List<Team> teamList) {
        try {
            return objectMapper.writeValueAsString(teamList);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("");
        }
    }

    // String -> List<Team>
    public static List<Team> deserializeTeamList(String teamListString) {
        if (teamListString == null || teamListString.isEmpty()) {
            throw new IllegalArgumentException("Пустая строка JSON");
        }

        try {
            CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, Team.class);
            return objectMapper.readValue(teamListString, listType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка десериализации Team: " + e.getMessage(), e);
        }
    }

}

