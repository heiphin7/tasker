package com.TaskService.service;

import com.TaskService.model.Task;

import java.util.List;

public interface TaskService {
    List<Task> processGetAllTasks(Long userId);
    Task getTaskById(Long taskId);
    void updateTask(Task task, Long taskId);
    void deleteTask(Long userId, Long taskId);
} 