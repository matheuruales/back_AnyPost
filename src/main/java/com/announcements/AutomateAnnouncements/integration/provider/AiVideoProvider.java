package com.announcements.AutomateAnnouncements.integration.provider;

import java.util.Optional;

/**
 * Adapter contract that hides the mechanics of a specific AI provider behind a uniform API.
 * Implementations translate our domain requests into the provider's native calls.
 */
public interface AiVideoProvider {

    VideoProviderType getType();

    /**
     * Requests an asynchronous video creation and returns the provider-specific job identifier.
     */
    String requestVideoCreation(String prompt, String style);

    /**
     * Checks whether an asynchronous creation already produced a playable video URL.
     */
    Optional<String> fetchVideoUrl(String creationId);

    /**
     * Generates a video in a blocking fashion and returns the final media URL.
     */
    String generateVideo(String prompt, String style);

    /**
     * Indicates whether the provider supports asynchronous creation flows.
     */
    default boolean supportsAsyncOperations() {
        return true;
    }
}
