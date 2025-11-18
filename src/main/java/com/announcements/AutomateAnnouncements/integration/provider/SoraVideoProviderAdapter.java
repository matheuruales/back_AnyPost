package com.announcements.AutomateAnnouncements.integration.provider;

import com.announcements.AutomateAnnouncements.integration.SoraIntegrationService;
import java.io.File;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapter for the mock Sora integration. Sora only exposes synchronous generations, so
 * asynchronous methods either return empty results or signal that the operation is unsupported.
 */
@Component
public class SoraVideoProviderAdapter implements AiVideoProvider {

    private final SoraIntegrationService soraIntegrationService;

    public SoraVideoProviderAdapter(SoraIntegrationService soraIntegrationService) {
        this.soraIntegrationService = soraIntegrationService;
    }

    @Override
    public VideoProviderType getType() {
        return VideoProviderType.SORA;
    }

    @Override
    public boolean supportsAsyncOperations() {
        return false;
    }

    @Override
    public String requestVideoCreation(String prompt, String style) {
        throw new UnsupportedOperationException("Sora does not support asynchronous generation");
    }

    @Override
    public Optional<String> fetchVideoUrl(String creationId) {
        return Optional.empty();
    }

    @Override
    public String generateVideo(String prompt, String style) {
        File file = soraIntegrationService.generateVideoFromPrompt(prompt);
        return file.toURI().toString();
    }
}
