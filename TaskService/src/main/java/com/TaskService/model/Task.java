package com.TaskService.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.Gson;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private TaskStatus status = TaskStatus.NEW;
    private TaskPriority priority;
    private Long creatorId;
    private Integer hours;
    private Integer minutes;
    private Long dueDate;
    private Long createdAt;
    private Long updatedAt;
    private Boolean isPersonal = true;

    public static Task fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Task.class);
    }
}