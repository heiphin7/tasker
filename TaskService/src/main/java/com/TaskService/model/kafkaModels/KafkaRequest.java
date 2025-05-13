package com.TaskService.model.kafkaModels;

import com.TaskService.dto.CreateSubtaskDto;
import com.TaskService.dto.CreateTeamTaskDto;
import com.TaskService.json.LocalDateTimeAdapter;
import com.TaskService.model.Subtask;
import com.TaskService.model.TeamTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@ToString
public class KafkaRequest {

    @SerializedName("subtaskId")
    private Long subtaskId;

    @SerializedName("subTask")
    private CreateSubtaskDto subtask;

    @SerializedName("editedTask")
    private TeamTask editedTeamTask;

    @SerializedName("team-tasks")
    private List<TeamTask> teamTasks;

    @SerializedName("teamId")
    private Long teamId;

    @SerializedName("dto")
    private CreateTeamTaskDto createTeamTaskDto;

    @SerializedName("taskId")
    private Long taskId;

    @SerializedName("requestId")
    private String requestId;

    @SerializedName("action")
    private String action;

    @SerializedName("userId")
    private Long userId;

    @SerializedName("response-status")
    private String responseStatus; // в JSON: response-status

    @SerializedName("response-message")
    private String responseMessage; // в JSON: response-message

    @SerializedName("tasks-empty")
    private Boolean taskEmpty; // в JSON: task-empty

    @SerializedName("tasks")
    private String task;

    @SerializedName("task-list")
    private String taskList;

    @SerializedName("taskToChangeId")
    private Long taskToChangeId;

    public static KafkaRequest fromJson(String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
        return gson.fromJson(json, KafkaRequest.class);
    }
}
