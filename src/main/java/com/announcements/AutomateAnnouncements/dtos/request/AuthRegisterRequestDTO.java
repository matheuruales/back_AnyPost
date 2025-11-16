package com.announcements.AutomateAnnouncements.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthRegisterRequestDTO {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String displayName;

    @NotBlank
    @Size(min = 6, message = "Password must contain at least 6 characters")
    private String password;
}
