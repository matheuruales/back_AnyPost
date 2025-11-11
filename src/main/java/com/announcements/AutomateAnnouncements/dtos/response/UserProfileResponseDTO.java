package com.announcements.AutomateAnnouncements.dtos.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserProfileResponseDTO {
    private Integer id;
    private String email;
    private String displayName;
    private LocalDateTime createdAt;
}