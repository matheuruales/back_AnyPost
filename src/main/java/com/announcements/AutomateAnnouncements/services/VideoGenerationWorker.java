package com.announcements.AutomateAnnouncements.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.announcements.AutomateAnnouncements.entities.VideoGenerationJob;
import com.announcements.AutomateAnnouncements.integration.provider.AiVideoProvider;
import com.announcements.AutomateAnnouncements.integration.provider.AiVideoProviderFactory;

import java.util.List;

@Slf4j
@Service
public class VideoGenerationWorker {

    @Autowired
    private VideoGenerationJobService jobService;

    @Autowired
    private AiVideoProviderFactory aiVideoProviderFactory;

    // Run every 30 seconds
    @Scheduled(fixedRate = 30000)
    public void processPendingJobs() {
        log.debug("Checking for pending video generation jobs...");

        List<VideoGenerationJob> pendingJobs = jobService.getPendingJobs();

        if (pendingJobs.isEmpty()) {
            log.debug("No pending jobs found");
            return;
        }

        log.info("Found {} pending jobs to process", pendingJobs.size());

        for (VideoGenerationJob job : pendingJobs) {
            try {
                processJob(job);
            } catch (Exception e) {
                log.error("Failed to process job {}: {}", job.getId(), e.getMessage());
                jobService.failJob(job.getId(), e.getMessage());
            }
        }
    }

    private void processJob(VideoGenerationJob job) {
        log.info("Processing job {} with creation ID: {}", job.getId(), job.getBlotatoCreationId());

        String creationId = job.getBlotatoCreationId();
        if (creationId == null || creationId.isBlank()) {
            log.error("Job {} does not have a valid Blotato creation ID. Marking job as failed.", job.getId());
            jobService.failJob(job.getId(), "Missing Blotato creation ID");
            return;
        }

        AiVideoProvider provider = aiVideoProviderFactory.getAsyncProvider();
        String videoUrl = provider.fetchVideoUrl(creationId).orElse(null);

        if (videoUrl != null) {
            // Video is ready
            log.info("Video for job {} is ready: {}", job.getId(), videoUrl);
            jobService.completeJob(job.getId(), videoUrl);
        } else {
            // Still processing
            log.debug("Video for job {} still processing", job.getId());
        }
    }
}
