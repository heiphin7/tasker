package com.UserService.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateTeamRequest {
    
    @NotBlank(message = "Название команды не может быть пустым")
    private String name;
    
    private String description;
    
    private Long managerId;
    
    private Set<Long> memberIds;
} 