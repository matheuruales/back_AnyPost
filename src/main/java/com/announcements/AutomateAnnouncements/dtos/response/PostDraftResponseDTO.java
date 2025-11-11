package com.announcements.AutomateAnnouncements.dtos.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PostDraftResponseDTO {
    private Integer id;
    private String title;
    private String description;
    private Integer assetId;
    private String targets;
    private String status;
    private LocalDateTime createdAt;
}