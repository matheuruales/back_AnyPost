package com.announcements.AutomateAnnouncements.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PostPublicationRequestDTO {

    @NotBlank
    @Size(max = 100)
    private String network;

    @Size(max = 2048)
    private String publishedUrl;

    @NotBlank
    @Size(max = 60)
    private String status;

    private LocalDateTime publishedAt;
}
