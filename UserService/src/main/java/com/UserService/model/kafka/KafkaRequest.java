package com.UserService.model.kafka;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class KafkaRequest {


    @SerializedName("nextAdmin")
    private Long nextAdmin;

    @SerializedName("username")
    private String username;

    @SerializedName("memberId")
    private Long memberId;

    @SerializedName("team-members")
    private String members;

    @SerializedName("teamId")
    private Long teamId;

    @SerializedName("createTaskDto")
    private String createTaskDto;

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
    private String tasks; // Сначала идет как просто строка, так как она сериализована

    public static KafkaRequest fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, KafkaRequest.class);
    }

}