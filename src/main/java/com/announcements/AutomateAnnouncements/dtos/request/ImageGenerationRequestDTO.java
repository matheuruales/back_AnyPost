package com.announcements.AutomateAnnouncements.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImageGenerationRequestDTO {

    @NotBlank
    private String prompt;

    private String size;

    private String quality;

    private String style;
}
