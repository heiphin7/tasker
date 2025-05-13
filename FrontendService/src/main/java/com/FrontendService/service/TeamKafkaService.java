package com.FrontendService.service;

import com.FrontendService.model.Team;
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
public class TeamKafkaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<Long, Team> teamCache = new ConcurrentHashMap<>();

    // Слушатели для обновления кэша команд
    @KafkaListener(topics = "team-created", groupId = "frontend-service-group")
    public void handleTeamCreated(Team team) {
        log.info("Получено событие о создании команды: {}", team);
        teamCache.put(team.getId(), team);
    }

    @KafkaListener(topics = "team-updated", groupId = "frontend-service-group")
    public void handleTeamUpdated(Team team) {
        log.info("Получено событие об обновлении команды: {}", team);
        teamCache.put(team.getId(), team);
    }

    @KafkaListener(topics = "team-deleted", groupId = "frontend-service-group")
    public void handleTeamDeleted(Long teamId) {
        log.info("Получено событие об удалении команды: {}", teamId);
        teamCache.remove(teamId);
    }

    // Методы для получения данных из кэша
    public Mono<List<Team>> getAllTeams() {
        return Mono.just(new ArrayList<>(teamCache.values()));
    }

    public Mono<Team> getTeamById(Long teamId) {
        return Mono.justOrEmpty(teamCache.get(teamId));
    }

    // Методы для отправки событий через Kafka
    public Mono<Team> createTeam(Team team) {
        return Mono.fromCallable(() -> {
            log.info("Отправка события создания команды через Kafka: {}", team);
            kafkaTemplate.send("team-created", team);
            return team;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Team> updateTeam(Long id, Team team) {
        return Mono.fromCallable(() -> {
            log.info("Отправка события обновления команды через Kafka: {}", team);
            kafkaTemplate.send("team-updated", team);
            return team;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteTeam(Long id) {
        return Mono.fromRunnable(() -> {
            log.info("Отправка события удаления команды через Kafka: {}", id);
            kafkaTemplate.send("team-deleted", id);
        });
    }

    // Методы для обновления кэша
    public void updateTeamCache(List<Team> teams) {
        teamCache.clear();
        teams.forEach(team -> teamCache.put(team.getId(), team));
    }
} 