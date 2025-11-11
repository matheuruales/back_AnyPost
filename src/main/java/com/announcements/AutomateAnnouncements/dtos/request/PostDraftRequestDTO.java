package com.announcements.AutomateAnnouncements.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PostDraftRequestDTO {
    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Integer assetId;

    @NotBlank
    private String targets;

    private String status;
}