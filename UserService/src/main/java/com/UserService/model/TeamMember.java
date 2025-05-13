package com.UserService.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMember {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private TeamRole role;
}