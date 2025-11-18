package com.announcements.AutomateAnnouncements.services;

import com.announcements.AutomateAnnouncements.dtos.request.VideoGenerationRequest;
import com.announcements.AutomateAnnouncements.entities.UserProfile;
import com.announcements.AutomateAnnouncements.entities.VideoGenerationJob;
import com.announcements.AutomateAnnouncements.integration.provider.AiVideoProvider;
import com.announcements.AutomateAnnouncements.integration.provider.AiVideoProviderFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Facade pattern – simplifies controllers by orchestrating multiple services and provider lookups
 * whenever we need to upload or generate media content.
 */
@Component
public class MediaProcessingFacade {

    private final VideoService videoService;
    private final VideoGenerationJobService videoGenerationJobService;
    private final AiVideoProviderFactory aiVideoProviderFactory;

    public MediaProcessingFacade(VideoService videoService,
                                 VideoGenerationJobService videoGenerationJobService,
                                 AiVideoProviderFactory aiVideoProviderFactory) {
        this.videoService = videoService;
        this.videoGenerationJobService = videoGenerationJobService;
        this.aiVideoProviderFactory = aiVideoProviderFactory;
    }

    public String uploadUserVideo(UserProfile userProfile,
                                  MultipartFile file,
                                  String title,
                                  String description,
                                  String targets) {
        return videoService.uploadUserVideo(userProfile, file, title, description, targets);
    }

    public VideoGenerationJob enqueueVideoGeneration(UserProfile owner, VideoGenerationRequest request) {
        VideoGenerationJob job = videoGenerationJobService.createJob(
                owner.getId(),
                request.getPrompt(),
                request.getTitle(),
                request.getDescription(),
                request.getTargets(),
                request.getStyle());

        AiVideoProvider provider = aiVideoProviderFactory.getAsyncProvider();
        String creationId = provider.requestVideoCreation(request.getPrompt(), request.getStyle());
        videoGenerationJobService.updateJobStatus(job.getId(), "PROCESSING", creationId);
        return job;
    }

    public String generateVideoSynchronously(UserProfile owner, VideoGenerationRequest request) {
        AiVideoProvider provider = aiVideoProviderFactory.getDefaultProvider();
        return videoService.generateVideoFromPrompt(
                request.getPrompt(),
                request.getTitle(),
                request.getDescription(),
                owner,
                request.getTargets(),
                request.getStyle(),
                provider);
    }
}
