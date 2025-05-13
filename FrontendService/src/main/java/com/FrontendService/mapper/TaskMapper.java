package com.FrontendService.mapper;

import com.FrontendService.model.Task;
import com.FrontendService.model.dto.CreateTaskDTO;
import org.springframework.stereotype.Service;

@Service
public class TaskMapper {
    public static Task toTask(CreateTaskDTO dto) {
        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setPriority(dto.getPriority());
        task.setDueDate(dto.getDueDate());
        task.setHours(dto.getHours());
        task.setMinutes(dto.getMinutes());

        return task;
    }
}
