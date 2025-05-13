package com.UserService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateTeamRequest {
    private String name;
    private String description;
    private Long managerId;
    private Set<Long> memberIds;
} 