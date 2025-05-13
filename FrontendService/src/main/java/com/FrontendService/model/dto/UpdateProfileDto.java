package com.FrontendService.model.dto;

import lombok.Data;

@Data
public class UpdateProfileDto {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
}
