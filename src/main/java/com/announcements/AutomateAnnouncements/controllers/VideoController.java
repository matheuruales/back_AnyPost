package com.announcements.AutomateAnnouncements.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.announcements.AutomateAnnouncements.dtos.request.VideoGenerationRequest;
import com.announcements.AutomateAnnouncements.services.MediaProcessingFacade;
import com.announcements.AutomateAnnouncements.services.VideoGenerationJobService;
import com.announcements.AutomateAnnouncements.entities.UserProfile;
import com.announcements.AutomateAnnouncements.entities.VideoGenerationJob;
import com.announcements.AutomateAnnouncements.security.AuthenticatedUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/videos")
@Tag(name = "Video Management", description = "Endpoints for video upload and generation")
public class VideoController {

    @Autowired
    private MediaProcessingFacade mediaProcessingFacade;

    @Autowired
    private VideoGenerationJobService videoGenerationJobService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @PostMapping("/upload")
    @Operation(summary = "Upload user video", description = "Uploads a video file, stores it in blob storage, creates database records, and sends data to n8n")
    public ResponseEntity<String> uploadVideo(
            @Parameter(description = "Video file to upload") @RequestParam("file") @NotNull MultipartFile file,
            @Parameter(description = "Title of the video") @RequestParam("title") @NotBlank String title,
            @Parameter(description = "Description of the video") @RequestParam("description") String description,
            @Parameter(description = "Auth User ID from Firebase") @RequestParam("authUserId") @NotBlank String authUserId,
            @Parameter(description = "Target platforms (comma-separated)") @RequestParam("targets") @NotBlank String targets) {

        log.info("Received video upload request: file={}, title={}, authUserId={}, targets={}", file.getOriginalFilename(), title, authUserId, targets);

        try {
            UserProfile currentUser = authenticatedUserService.getCurrentUser();
            if (!currentUser.getAuthUserId().equals(authUserId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only upload videos for your own profile");
            }

            String videoUrl = mediaProcessingFacade.uploadUserVideo(currentUser, file, title, description, targets);
            log.info("Video uploaded successfully: {}", videoUrl);

            return ResponseEntity.status(201).body("Video uploaded successfully. URL: " + videoUrl);
        } catch (Exception e) {
            log.error("Failed to upload video: {}", e.getMessage());
            return ResponseEntity.status(500).body("Failed to upload video: " + e.getMessage());
        }
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate video from prompt (Async)", description = "Creates an async job to generate a video using Blotato AI from a text prompt. Returns job ID immediately.")
    public ResponseEntity<String> generateVideo(
            @Parameter(description = "Text prompt for video generation") @RequestParam("prompt") @NotBlank String prompt,
            @Parameter(description = "Title of the generated video") @RequestParam("title") @NotBlank String title,
            @Parameter(description = "Description of the generated video") @RequestParam("description") String description,
            @Parameter(description = "Owner ID") @RequestParam("ownerId") @NotNull Integer ownerId,
            @Parameter(description = "Target platforms (comma-separated)") @RequestParam("targets") @NotBlank String targets,
            @Parameter(description = "Video style (optional)") @RequestParam(value = "style", required = false) String style) {

        log.info("Received async video generation request: prompt={}, title={}, ownerId={}, targets={}, style={}", prompt, title, ownerId, targets, style);

        try {
            UserProfile currentUser = authenticatedUserService.getCurrentUser();
            if (!currentUser.getId().equals(ownerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only generate videos for your profile");
            }

            VideoGenerationRequest request = VideoGenerationRequest.builder()
                    .withPrompt(prompt)
                    .withTitle(title)
                    .withDescription(description)
                    .withTargets(targets)
                    .withStyle(style)
                    .build();

            VideoGenerationJob job = mediaProcessingFacade.enqueueVideoGeneration(currentUser, request);
            log.info("Created async video generation job with ID: {}", job.getId());

            return ResponseEntity.status(202).body("Video generation job created. Job ID: " + job.getId());
        } catch (Exception e) {
            log.error("Failed to create video generation job: {}", e.getMessage());
            return ResponseEntity.status(500).body("Failed to create video generation job: " + e.getMessage());
        }
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get job status", description = "Check the status of a video generation job")
    public ResponseEntity<?> getJobStatus(@Parameter(description = "Job ID") @PathVariable Integer jobId) {
        UserProfile currentUser = authenticatedUserService.getCurrentUser();
        var jobOpt = videoGenerationJobService.getJobById(jobId);

        if (jobOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var job = jobOpt.get();
        if (!job.getOwnerId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Job does not belong to the current user");
        }
        Map<String, Object> response = new HashMap<>();
        response.put("jobId", job.getId());
        response.put("status", job.getStatus());
        response.put("createdAt", job.getCreatedAt());
        response.put("updatedAt", job.getUpdatedAt());
        response.put("videoUrl", job.getVideoUrl());
        response.put("errorMessage", job.getErrorMessage());

        return ResponseEntity.ok(response);
    }
}
