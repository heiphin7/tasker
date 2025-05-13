package com.UserService.dto;

import com.google.gson.Gson;
import lombok.Data;

import java.util.List;

@Data
public class CreateTeamDTO {
    private String title;
    private String description;
    private List<String> membersName;
    private Boolean isCorporate;

    public static CreateTeamDTO fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, CreateTeamDTO.class);
    }

}


