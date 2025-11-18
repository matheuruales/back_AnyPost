package com.announcements.AutomateAnnouncements.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.announcements.AutomateAnnouncements.entities.VideoGenerationJob;
import com.announcements.AutomateAnnouncements.entities.UserProfile;
import com.announcements.AutomateAnnouncements.repositories.VideoGenerationJobRepository;
import com.announcements.AutomateAnnouncements.repositories.UserProfileRepository;
import com.announcements.AutomateAnnouncements.dtos.response.AssetResponseDTO;
import com.announcements.AutomateAnnouncements.dtos.response.PostDraftResponseDTO;
import com.announcements.AutomateAnnouncements.integration.N8nIntegrationService;
import com.announcements.AutomateAnnouncements.dtos.request.UserPostRequestDTO;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class VideoGenerationJobService {

    @Autowired
    private VideoGenerationJobRepository jobRepository;

    @Autowired
    private AssetService assetService;

    @Autowired
    private PostDraftService postDraftService;

    @Autowired
    private N8nIntegrationService n8nIntegrationService;

    @Autowired
    private UserPostService userPostService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Transactional
    public VideoGenerationJob createJob(Integer ownerId, String prompt, String title,
                                      String description, String targets, String style) {
        VideoGenerationJob job = new VideoGenerationJob();
        job.setOwnerId(ownerId);
        job.setPrompt(prompt);
        job.setTitle(title);
        job.setDescription(description);
        job.setTargets(targets);
        job.setStyle(style);
        job.setStatus("QUEUED");
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());

        VideoGenerationJob savedJob = jobRepository.save(job);
        log.info("Created video generation job with ID: {}", savedJob.getId());

        return savedJob;
    }

    @Transactional
    public void updateJobStatus(Integer jobId, String status, String blotatoCreationId) {
        Optional<VideoGenerationJob> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isPresent()) {
            VideoGenerationJob job = jobOpt.get();
            job.setStatus(status);
            job.setBlotatoCreationId(blotatoCreationId);
            job.setUpdatedAt(LocalDateTime.now());

            if ("COMPLETED".equals(status)) {
                job.setCompletedAt(LocalDateTime.now());
            }

            jobRepository.save(job);
            log.info("Updated job {} status to: {}", jobId, status);
        }
    }

    @Transactional
    public void completeJob(Integer jobId, String videoUrl) {
        Optional<VideoGenerationJob> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isPresent()) {
            VideoGenerationJob job = jobOpt.get();

            UserProfile userProfile = userProfileRepository.findById(job.getOwnerId())
                    .orElseThrow(() -> new RuntimeException("User profile not found with ID: " + job.getOwnerId()));

            // Create asset record
            com.announcements.AutomateAnnouncements.dtos.request.AssetRequestDTO assetRequest =
                new com.announcements.AutomateAnnouncements.dtos.request.AssetRequestDTO();
            assetRequest.setOwner(job.getOwnerId());
            assetRequest.setType("video");
            assetRequest.setSource("generated.mp4");
            assetRequest.setBlobUrl(videoUrl);

            AssetResponseDTO asset = assetService.create(assetRequest);

            // Create post draft
            com.announcements.AutomateAnnouncements.dtos.request.PostDraftRequestDTO postDraftRequest =
                new com.announcements.AutomateAnnouncements.dtos.request.PostDraftRequestDTO();
            postDraftRequest.setTitle(job.getTitle());
            postDraftRequest.setDescription(job.getDescription());
            postDraftRequest.setAssetId(asset.getId());
            postDraftRequest.setTargets(job.getTargets());
            postDraftRequest.setStatus("pending");

            PostDraftResponseDTO postDraft = postDraftService.create(postDraftRequest);

            // Send to n8n
            n8nIntegrationService.sendVideoToN8n(
                job.getTitle(), job.getDescription(), videoUrl, job.getTargets());

            // Create UserPost entry for the generated video
            String authUserId = userProfile.getAuthUserId();
            if (authUserId == null || authUserId.isBlank()) {
                throw new RuntimeException("User profile " + job.getOwnerId() + " does not have an authUserId");
            }

            UserPostRequestDTO userPostRequestDTO = new UserPostRequestDTO();
            userPostRequestDTO.setTitle(job.getTitle());
            userPostRequestDTO.setContent(job.getDescription());
            userPostRequestDTO.setVideoUrl(videoUrl);
            userPostRequestDTO.setStatus("published");

            if (job.getTargets() != null && !job.getTargets().isBlank()) {
                List<String> targets = Arrays.stream(job.getTargets().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
                userPostRequestDTO.setTargetPlatforms(targets);
            }

            userPostService.createPost(userProfile, userPostRequestDTO);

            job.setVideoUrl(videoUrl);
            job.setAssetId(asset.getId());
            job.setPostDraftId(postDraft.getId());
            job.setStatus("COMPLETED");
            job.setCompletedAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());

            jobRepository.save(job);
            log.info("Completed job {} with video URL: {}", jobId, videoUrl);
        }
    }

    @Transactional
    public void failJob(Integer jobId, String errorMessage) {
        Optional<VideoGenerationJob> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isPresent()) {
            VideoGenerationJob job = jobOpt.get();
            job.setStatus("FAILED");
            job.setErrorMessage(errorMessage);
            job.setUpdatedAt(LocalDateTime.now());

            jobRepository.save(job);
            log.error("Failed job {} with error: {}", jobId, errorMessage);
        }
    }

    public List<VideoGenerationJob> getPendingJobs() {
        return jobRepository.findPendingJobs(Arrays.asList("QUEUED", "PROCESSING"));
    }

    public Optional<VideoGenerationJob> getJobById(Integer jobId) {
        return jobRepository.findById(jobId);
    }

    public List<VideoGenerationJob> getJobsByOwner(Integer ownerId) {
        return jobRepository.findByOwnerId(ownerId);
    }
}
