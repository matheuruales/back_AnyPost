package com.announcements.AutomateAnnouncements.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssetRequestDTO {
    @NotNull
    private Integer owner;

    @NotBlank
    private String type;

    private String source;

    @NotBlank
    private String blobUrl;
}