package com.announcements.AutomateAnnouncements.dtos.response;

import lombok.Data;

@Data
public class PublicationResultResponseDTO {
    private Integer id;
    private String network;
    private String status;
    private String url;
    private String error;
}