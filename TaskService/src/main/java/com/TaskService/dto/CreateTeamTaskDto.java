package com.TaskService.dto;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class CreateTeamTaskDto {
    private String title;
    private String description;
    private Long teamId;
    private Long assigneeId;
    private LocalDateTime dueDate;
}

