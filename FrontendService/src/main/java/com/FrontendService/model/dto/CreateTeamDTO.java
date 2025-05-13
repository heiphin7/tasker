package com.FrontendService.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateTeamDTO {
    private String title;
    private String description;
    private List<String> membersName;
    private Boolean isCorporate;
}
