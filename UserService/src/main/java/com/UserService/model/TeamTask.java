package com.UserService.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamTask {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private Long creatorId;
    private Long assigneeId;
    private Long teamId;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private boolean isPersonal;
    private boolean isRecurring;
    private RecurrencePattern recurrencePattern;
    private Integer recurrenceInterval;
    private List<Subtask> subtasks = new ArrayList<>();
    private List<TaskComment> comments = new ArrayList<>();
    private List<TaskAttachment> attachments = new ArrayList<>();
    private Set<Long> watchers = new HashSet<>();
}
