package com.announcements.AutomateAnnouncements.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserProfileRequestDTO {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String displayName;
}