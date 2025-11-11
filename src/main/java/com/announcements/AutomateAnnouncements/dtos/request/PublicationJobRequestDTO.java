package com.announcements.AutomateAnnouncements.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PublicationJobRequestDTO {
    @NotNull
    private Integer postDraftId;

    private String status;
}