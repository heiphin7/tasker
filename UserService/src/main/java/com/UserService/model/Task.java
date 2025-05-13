package com.UserService.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Task {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status = TaskStatus.NEW;
    private TaskPriority priority;
    private Long creatorId;

    @DateTimeFormat(pattern = "yyyy-MM-dd")  // Указываем формат даты
    private Date dueDate;
    private Integer hours;
    private Integer minutes;
    private Date createdAt = new Date();
    private Date updatedAt;
    private Boolean isPersonal = true;

    private List<Subtask> subtasks = new ArrayList<>();
}