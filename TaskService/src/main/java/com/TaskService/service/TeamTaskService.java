package com.TaskService.service;

import com.TaskService.dto.CreateSubtaskDto;
import com.TaskService.dto.CreateTeamTaskDto;
import com.TaskService.model.Subtask;
import com.TaskService.model.TeamTask;
import com.TaskService.model.kafkaModels.KafkaRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface TeamTaskService {
    void createTeamTask(Long userId, Long teamId, CreateTeamTaskDto dto);
    void delete(Long userId, Long teamId, Long teamTaskId);
    void update(Long userId, Long teamId, TeamTask updatedTask);
    List<TeamTask> getAllTeamTasks(Long teamId);
    void updateTeamTask(TeamTask editedTask);
    void deleteTeamTask(Long teamId, Long taskId) throws IllegalArgumentException;
    void addTeamSubTask(CreateSubtaskDto subtask, Long userId, Long teamId, Long taskId);
    void deleteTeamSubTask(Long subtaskId, Long userId, Long teamId, Long taskId);
    void editTeamSubtask(CreateSubtaskDto updatedSubtask, Long userId, Long teamId, Long taskId);
}
