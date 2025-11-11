package com.announcements.AutomateAnnouncements.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PublicationResultRequestDTO {
    @NotBlank
    private String network;

    @NotBlank
    private String status;

    private String url;

    private String error;
}