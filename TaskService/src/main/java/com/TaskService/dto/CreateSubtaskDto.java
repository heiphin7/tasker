package com.TaskService.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateSubtaskDto {
    private Long id;
    private Long taskId;
    private String title;
    private String description;
    private boolean completed;
    private Long assigneeId;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
