package com.UserService.service;

import com.UserService.model.Team;
import com.UserService.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            throw new RuntimeException("Ошибка при сериализации User в строку: " + e.getMessage(), e);
        }
    }

    // String -> User
    public static User deserializeUser(String userString) {
        try {
            return objectMapper.readValue(userString, User.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка при десериализации строки в User: " + e.getMessage(), e);
        }
    }

    // List<User> -> String
    public static String serializeUserList(List<User> userList) {
        try {
            return objectMapper.writeValueAsString(userList);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка при сериализации списка User в строку: " + e.getMessage(), e);
        }
    }

    // String -> List<User>
    public static List<User> deserializeUserList(String taskListString) {
        try {
            return objectMapper.readValue(taskListString,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, User.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка при десериализации строки в список User: " + e.getMessage(), e);
        }
    }

    // Team -> String
    public static String serializeTeam(Team team) {
        try {
            return objectMapper.writeValueAsString(team);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка при сериализации Team в строку: " + e.getMessage(), e);
        }
    }

    // String -> Team
    public static Team deserializeTeam(String teamString) {
        try {
            return objectMapper.readValue(teamString, Team.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка при десериализации строки в Team: " + e.getMessage(), e);
        }
    }

    // List<Team> -> String
    public static String serializeTeamList(List<Team> teamList) {
        try {
            return objectMapper.writeValueAsString(teamList);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка при сериализации списка Team в строку: " + e.getMessage(), e);
        }
    }

    // String -> List<Team>
    public static List<Team> deserializeTeamList(String teamListString) {
        try {
            return objectMapper.readValue(teamListString,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Team.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка при десериализации строки в список Team: " + e.getMessage(), e);
        }
    }

}
