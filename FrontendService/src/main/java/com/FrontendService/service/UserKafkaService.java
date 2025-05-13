package com.FrontendService.service;

import com.FrontendService.model.Team;
import com.FrontendService.model.User;
import com.FrontendService.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserKafkaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JwtUtil jwtUtil;
    private final JwtService jwtService;
    private final Map<String, User> userCache = new ConcurrentHashMap<>();
    private final Map<String, List<Team>> userTeamsCache = new ConcurrentHashMap<>();

    // Слушатели для обновления кэша пользователей
    @KafkaListener(topics = "user-created", groupId = "frontend-service-group")
    public void handleUserCreated(User user) {
        log.info("Получено событие о создании пользователя: {}", user);
        userCache.put(user.getUsername(), user);
    }

    @KafkaListener(topics = "user-updated", groupId = "frontend-service-group")
    public void handleUserUpdated(User user) {
        log.info("Получено событие об обновлении пользователя: {}", user);
        userCache.put(user.getUsername(), user);
    }

    @KafkaListener(topics = "user-deleted", groupId = "frontend-service-group")
    public void handleUserDeleted(String username) {
        log.info("Получено событие об удалении пользователя: {}", username);
        userCache.remove(username);
        userTeamsCache.remove(username);
    }

    // Слушатели для обновления связей пользователь-команда
    @KafkaListener(topics = "user-added-to-team", groupId = "frontend-service-group")
    public void handleUserAddedToTeam(Map<String, Object> event) {
        log.info("Получено событие о добавлении пользователя в команду: {}", event);
        String username = (String) event.get("username");
        Long teamId = Long.valueOf(event.get("teamId").toString());
        
        Team team = (Team) event.get("team");
        if (team != null) {
            userTeamsCache.computeIfAbsent(username, k -> new ArrayList<>()).add(team);
        }
    }

    @KafkaListener(topics = "user-removed-from-team", groupId = "frontend-service-group")
    public void handleUserRemovedFromTeam(Map<String, Object> event) {
        log.info("Получено событие об удалении пользователя из команды: {}", event);
        String username = (String) event.get("username");
        Long teamId = Long.valueOf(event.get("teamId").toString());
        
        List<Team> userTeams = userTeamsCache.get(username);
        if (userTeams != null) {
            userTeams.removeIf(team -> team.getId().equals(teamId));
        }
    }

    // Методы для получения данных из кэша
    public Mono<User> getCurrentUser(String token) {
        try {
            String username = jwtService.extractUsername(token);
            List<String> roles = jwtService.extractRoles(token);

            // Создаем объект User на основе данных из токена.
            User user = new User();
            user.setUsername(username);
            user.setRoles(roles);

            return Mono.just(user);

        } catch (Exception e) {
            // Обработка ошибок, если токен невалиден или данные не могут быть извлечены.
            return Mono.error(new RuntimeException("Invalid token or user data extraction failed: " + e.getMessage()));
        }
    }

    public Mono<User> getUserById(Long userId) {
        return Mono.justOrEmpty(userCache.values().stream()
                .filter(user -> user.getId().equals(userId))
                .findFirst());
    }

    public Mono<List<User>> getAllUsers() {
        return Mono.just(new ArrayList<>(userCache.values()));
    }

    public Mono<List<Team>> getUserTeams(String username) {
        return Mono.justOrEmpty(userTeamsCache.get(username));
    }

    // Методы для обновления кэша
    public void updateUserCache(List<User> users) {
        userCache.clear();
        users.forEach(user -> userCache.put(user.getUsername(), user));
    }

    public void updateUserTeamsCache(String username, List<Team> teams) {
        userTeamsCache.put(username, teams);
    }

    public Mono<User> updateUser(User user) {
        return Mono.fromCallable(() -> {
            log.info("Отправка события обновления пользователя через Kafka: {}", user);
            kafkaTemplate.send("user-updated", user);
            return user;
        }).subscribeOn(Schedulers.boundedElastic());
    }
} 