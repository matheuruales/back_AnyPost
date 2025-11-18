package com.announcements.AutomateAnnouncements.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class VideoGenerationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer ownerId;
    private String prompt;
    private String title;
    private String description;
    private String targets;
    private String style;

    // Blotato specific fields
    private String blotatoCreationId;
    private String status; // QUEUED, PROCESSING, COMPLETED, FAILED

    // Result fields
    private String videoUrl;
    private String errorMessage;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    // Asset and PostDraft IDs when completeddd
    private Integer assetId;
    private Integer postDraftId;
}