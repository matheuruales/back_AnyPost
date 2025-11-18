package com.announcements.AutomateAnnouncements.services.listeners;

import com.announcements.AutomateAnnouncements.entities.VideoGenerationJob;

/**
 * Observer pattern – listeners implement this contract to react when a video generation job
 * finishes or fails without the job service knowing about the concrete side-effects.
 */
public interface VideoJobListener {

    default void onJobCompleted(VideoGenerationJob job) {}

    default void onJobFailed(VideoGenerationJob job) {}
}
