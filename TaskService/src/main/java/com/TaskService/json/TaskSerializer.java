package com.TaskService.json;

import com.TaskService.model.Task;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskSerializer {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 🟢 Task -> String
    public static String serializeTask(Task task) {
        try {
            return objectMapper.writeValueAsString(task);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка сериализации Task в строку", e);
        }
    }

    // 🟢 String -> Task
    public static Task deserializeTask(String taskString) {
        try {
            return objectMapper.readValue(taskString, Task.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка десериализации строки в Task", e);
        }
    }

    // 🟢 List<Task> -> String
    public static String serializeTaskList(List<Task> tasks) {
        try {
            return objectMapper.writeValueAsString(tasks);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка сериализации List<Task> в строку", e);
        }
    }

    // 🟢 String -> List<Task>
    public static List<Task> deserializeTaskList(String taskListString) {
        try {
            return objectMapper.readValue(taskListString,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Task.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка десериализации строки в List<Task>", e);
        }
    }
}
