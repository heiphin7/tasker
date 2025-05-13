package com.TaskService.service.impl;

import com.TaskService.kafka.TaskEventProducer;
import com.TaskService.model.Task;
import com.TaskService.repository.TaskRepository;
import com.TaskService.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    @Override
    public List<Task> processGetAllTasks(Long userId) {
        return taskRepository.getTasksByCreatorId(userId);
    }

    @Override
    public Task getTaskById(Long taskId) {
        return taskRepository.findById(taskId).orElse(null);
    }

    @Override
    public void updateTask(Task newTask, Long taskId) {
        taskRepository.findById(taskId).ifPresent(existingTask -> {
            if (newTask.getTitle() != null && !newTask.getTitle().equals(existingTask.getTitle())) {
                existingTask.setTitle(newTask.getTitle());
            }
            if (newTask.getDescription() != null && !newTask.getDescription().equals(existingTask.getDescription())) {
                existingTask.setDescription(newTask.getDescription());
            }
            if (newTask.getStatus() != null && !newTask.getStatus().equals(existingTask.getStatus())) {
                existingTask.setStatus(newTask.getStatus());
            }
            if (newTask.getPriority() != null && !newTask.getPriority().equals(existingTask.getPriority())) {
                existingTask.setPriority(newTask.getPriority());
            }
            if (newTask.getDueDate() != null && !newTask.getDueDate().equals(existingTask.getDueDate())) {
                existingTask.setDueDate(newTask.getDueDate());
            }
            if (newTask.getHours() != null && !newTask.getHours().equals(existingTask.getHours())) {
                existingTask.setHours(newTask.getHours());
            }
            if (newTask.getMinutes() != null && !newTask.getMinutes().equals(existingTask.getMinutes())) {
                existingTask.setMinutes(newTask.getMinutes());
            }
            if (newTask.getIsPersonal() != null && !newTask.getIsPersonal().equals(existingTask.getIsPersonal())) {
                existingTask.setIsPersonal(newTask.getIsPersonal());
            }
            existingTask.setUpdatedAt(Instant.now().toEpochMilli());
            taskRepository.save(existingTask);
        });
    }

    @Override
    public void deleteTask(Long userId, Long taskId) throws IllegalArgumentException {
        Task task = taskRepository.findById(taskId).orElseThrow(
                () -> new IllegalArgumentException("Такой задачи не сущесвует!")
        );

        if (!task.getCreatorId().equals(userId)) {
            throw new IllegalArgumentException("Задача не принадлежит вам!");
        }

        taskRepository.delete(task);
    }

}