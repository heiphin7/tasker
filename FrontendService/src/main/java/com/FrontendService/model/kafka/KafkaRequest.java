package com.FrontendService.model.kafka;

import com.FrontendService.model.TeamTask;
import com.FrontendService.model.dto.CreateTeamTaskDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.util.List;

@Getter
public class KafkaRequest {

    @SerializedName("notifications")
    @JsonProperty("notifications")
    private Integer notifications;

    @SerializedName("team-tasks")
    @JsonProperty("team-tasks")
    private List<TeamTask> teamTasks;

    @SerializedName("createTeamTaskDto")
    @JsonProperty("createTeamTaskDto")
    private CreateTeamTaskDto createTeamTaskDto;

    @SerializedName("team-members")
    @JsonProperty("team-members")
    private String members;

    @SerializedName("teamId")
    @JsonProperty("teamId")
    private Long teamId;

    @SerializedName("serializedTeamList")
    @JsonProperty("serializedTeamList")
    private String serializedTeamList;

    @SerializedName("response-empty")
    @JsonProperty("response-empty")
    private String responseEmpty;

    @SerializedName("teamInfo")
    @JsonProperty("teamInfo")
    private String teamInfo;

    @SerializedName("requestId")
    @JsonProperty("requestId")
    private String requestId;

    @SerializedName("action")
    @JsonProperty("action")
    private String action;

    @SerializedName("userId")
    @JsonProperty("userId")
    private Long userId;

    @SerializedName("response-status")
    @JsonProperty("response-status")
    private String responseStatus;

    @SerializedName("response-message")
    @JsonProperty("response-message")
    private String responseMessage;

    @SerializedName("tasks-empty")
    @JsonProperty("tasks-empty")
    private Boolean taskEmpty;

    @SerializedName("tasks")
    @JsonProperty("tasks")
    private String tasks;

    public static KafkaRequest fromJson(String json) {
        return new Gson().fromJson(json, KafkaRequest.class);
    }
}

