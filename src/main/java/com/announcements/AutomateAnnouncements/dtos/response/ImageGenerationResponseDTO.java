package com.announcements.AutomateAnnouncements.dtos.response;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ImageGenerationResponseDTO {

    private String prompt;

    private String revisedPrompt;

    private String imageUrl;

    private String size;

    private String quality;

    private String style;

    private LocalDateTime generatedAt;

    private String blobUrl;
}
