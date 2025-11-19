package com.announcements.AutomateAnnouncements.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class UserPostRequestDTO {
    @NotBlank
    @Size(max = 255)
    private String title;

    private String content;

    @Size(max = 60)
    private String status;

    @Size(max = 2048)
    private String videoUrl;

    @Size(max = 2048)
    private String imageUrl;

    private LocalDateTime publishedAt;

    private List<@Size(max = 50) String> tags;

    private List<@Size(max = 60) String> targetPlatforms;

    @Valid
    private List<PostPublicationRequestDTO> publications;
}
