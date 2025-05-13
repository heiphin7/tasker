package com.FrontendService.model.dto;

import com.FrontendService.model.Team;
import com.FrontendService.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamInfoDto {
    private Team team;
    private Boolean isCorporate;
    private List<User> members;
}
