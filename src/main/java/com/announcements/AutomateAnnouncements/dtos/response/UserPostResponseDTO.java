package com.announcements.AutomateAnnouncements.dtos.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class UserPostResponseDTO {
    private UUID id;
    private String title;
    private String content;
    private String status;
    private String videoUrl;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private String ownerAuthUserId;
    private List<String> tags;
    private List<String> targetPlatforms;
    private List<PostPublicationResponseDTO> publications;
}
