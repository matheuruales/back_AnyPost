package com.announcements.AutomateAnnouncements.dtos.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PublicationJobResponseDTO {
    private Integer id;
    private Integer postDraftId;
    private String status;
    private LocalDateTime requestedAt;
}