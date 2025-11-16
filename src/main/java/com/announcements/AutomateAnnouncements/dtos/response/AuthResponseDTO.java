package com.announcements.AutomateAnnouncements.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponseDTO {
    private String token;
    private UserProfileResponseDTO user;
}
