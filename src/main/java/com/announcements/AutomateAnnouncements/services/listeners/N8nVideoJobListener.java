package com.announcements.AutomateAnnouncements.services.listeners;

import com.announcements.AutomateAnnouncements.entities.VideoGenerationJob;
import com.announcements.AutomateAnnouncements.integration.N8nIntegrationService;
import org.springframework.stereotype.Component;

/**
 * Concrete observer that pushes finished jobs to n8n once the service notifies completion.
 */
@Component
public class N8nVideoJobListener implements VideoJobListener {

    private final N8nIntegrationService n8nIntegrationService;

    public N8nVideoJobListener(N8nIntegrationService n8nIntegrationService) {
        this.n8nIntegrationService = n8nIntegrationService;
    }

    @Override
    public void onJobCompleted(VideoGenerationJob job) {
        if (job.getVideoUrl() == null) {
            return;
        }
        n8nIntegrationService.sendVideoToN8n(
                job.getTitle(),
                job.getDescription(),
                job.getVideoUrl(),
                job.getTargets());
    }
}
