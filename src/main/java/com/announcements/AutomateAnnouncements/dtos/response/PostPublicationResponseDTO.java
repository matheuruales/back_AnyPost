package com.announcements.AutomateAnnouncements.dtos.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class PostPublicationResponseDTO {
    private UUID id;
    private String network;
    private String status;
    private String publishedUrl;
    private LocalDateTime publishedAt;
}
