package com.announcements.AutomateAnnouncements.services.listeners;

import com.announcements.AutomateAnnouncements.entities.VideoGenerationJob;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Observer pattern – keeps a list of {@link VideoJobListener}s and fans out job lifecycle
 * events to them so that new reactions can be added without changing the core service.
 */
@Component
public class VideoJobEventPublisher {

    private final List<VideoJobListener> listeners;

    public VideoJobEventPublisher(List<VideoJobListener> listeners) {
        this.listeners = listeners;
    }

    public void notifyJobCompleted(VideoGenerationJob job) {
        listeners.forEach(listener -> listener.onJobCompleted(job));
    }

    public void notifyJobFailed(VideoGenerationJob job) {
        listeners.forEach(listener -> listener.onJobFailed(job));
    }
}
