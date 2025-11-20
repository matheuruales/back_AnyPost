package com.announcements.AutomateAnnouncements.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiImagePublishRequestDTO {

    @NotBlank
    private String authUserId;

    @NotBlank
    private String blobUrl;

    @NotBlank
    private String title;

    private String description;

    /**
     * Conjunto de redes sociales donde se publicará el post, en CSV.
     * Se mantiene el formato actual para que el JSON hacia n8n sea idéntico.
     */
    @NotBlank
    private String targets;
}
