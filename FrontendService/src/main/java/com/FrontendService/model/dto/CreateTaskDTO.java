package com.FrontendService.model.dto;

import com.FrontendService.model.TaskPriority;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@NoArgsConstructor
public class CreateTaskDTO {
    private String title;
    private String description;
    private TaskPriority priority;

    @DateTimeFormat(pattern = "yyyy-MM-dd")  // Указываем формат даты
    private Date dueDate;
    private Integer hours;
    private Integer minutes;

    // Конструктор для простоты
    public CreateTaskDTO(String title, String description, TaskPriority priority, Date dueDate, int hours, int minutes) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.hours = hours;
        this.minutes = minutes;
        this.dueDate = dueDate;
    }

}

