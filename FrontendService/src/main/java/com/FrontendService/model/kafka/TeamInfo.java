package com.FrontendService.model.kafka;

import com.FrontendService.model.Team;
import com.FrontendService.model.TeamTask;
import com.FrontendService.model.User;
import lombok.Data;

import java.util.List;

@Data
public class TeamInfo {
    private List<TeamTask> teamTaskList;
    private List<User> membersList;
    private Team team;
}
