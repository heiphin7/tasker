package com.FrontendService.controller;

import com.FrontendService.mapper.TaskMapper;
import com.FrontendService.model.Task;
import com.FrontendService.model.dto.CreateTaskDTO;
import com.FrontendService.service.JwtService;
import com.FrontendService.service.TaskKafkaService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jdk.jfr.Frequency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskKafkaService taskKafkaService;
    private final JwtService jwtService;

    @GetMapping
    public String tasksPage(Model model, HttpServletRequest request) {
        model.addAttribute("createTaskDTO", new CreateTaskDTO());
        String token = jwtService.getRefreshByRequest(request);
        Long userId = token != null ? jwtService.getUserIdFromToken(token) : null;

        if (userId == null) {
            log.error("Не удалось получить userId из токена");
            return "redirect:/login";
        }

        List<Task> allTasks = taskKafkaService.getAllTasksByUserId(userId);
        model.addAttribute("tasks", allTasks != null ? allTasks : new ArrayList<>());
        log.info("Успешно получены список задач: " + allTasks);

        List<Task> newTasks = new ArrayList<>();
        List<Task> inProgressTasks = new ArrayList<>();
        List<Task> completedTasks = new ArrayList<>();

        if (allTasks != null) {
            for (Task task : allTasks) {
                switch (task.getStatus()) {
                    case NEW -> newTasks.add(task);
                    case IN_PROGRESS -> inProgressTasks.add(task);
                    case COMPLETED -> completedTasks.add(task);
                }
            }
        }

        Integer notifications = taskKafkaService.getUserNotifications(userId);
        model.addAttribute("notifications", notifications);

        model.addAttribute("newTasks", newTasks);
        model.addAttribute("inProgressTasks", inProgressTasks);
        model.addAttribute("completedTasks", completedTasks);

        return "tasks";
    }

    @PostMapping("/create")
    public String createTask(
            @ModelAttribute CreateTaskDTO taskDTO,  // Получаем данные из формы
            HttpServletRequest request) {
        log.info("Запрос на создание задачи");

        String token = jwtService.getRefreshByRequest(request);
        Long userId = token != null ? jwtService.getUserIdFromToken(token) : null;
        if (userId == null) {
            log.error("Не удалось получить userId из токена");
            return "redirect:/login";
        }

        Task newTask = TaskMapper.toTask(taskDTO);
        newTask.setCreatorId(userId);
        taskKafkaService.saveTask(newTask);
        log.info("Задача успешно создана: " + newTask);

        return "redirect:/tasks";
    }

    @PostMapping("/edit/{taskId}")
    public String editTask(
            @ModelAttribute Task task,
            HttpServletRequest request,
            @PathVariable(name = "taskId") Long taskToChange) {
        log.info("Запрос на изменение задачи: " + task.toString());

        String token = jwtService.getRefreshByRequest(request);
        Long userId = jwtService.getUserIdFromToken(token);
        taskKafkaService.updateTask(task, taskToChange, userId);
        return "redirect:/tasks";
    }

    @PostMapping("/delete/{taskId}")
    public String deleteTask(RedirectAttributes redirectAttributes,
                             @PathVariable("taskId") Long taskId,
                             HttpServletRequest request) {
        String token = jwtService.getRefreshByRequest(request);
        Long userId = jwtService.getUserIdFromToken(token);
        log.info("Запрос на удаление заадчи: " + userId + " " + taskId);

        String response = taskKafkaService.deleteTask(userId, taskId);

        if (response.startsWith("ERROR")) {
            redirectAttributes.addFlashAttribute("аerrorMessage", response.substring(6));
        } else {
            redirectAttributes.addFlashAttribute("message", response);
        }

        return "redirect:/tasks";
    }
}