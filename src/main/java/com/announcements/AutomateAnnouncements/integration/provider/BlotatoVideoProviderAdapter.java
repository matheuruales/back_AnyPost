package com.announcements.AutomateAnnouncements.integration.provider;

import com.announcements.AutomateAnnouncements.integration.BlotatoVideoService;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapter pattern – exposes {@link BlotatoVideoService} behind the {@link AiVideoProvider} interface
 * so the rest of the application does not need to know how Blotato's API works.
 */
@Component
public class BlotatoVideoProviderAdapter implements AiVideoProvider {

    private final BlotatoVideoService blotatoVideoService;

    public BlotatoVideoProviderAdapter(BlotatoVideoService blotatoVideoService) {
        this.blotatoVideoService = blotatoVideoService;
    }

    @Override
    public VideoProviderType getType() {
        return VideoProviderType.BLOTATO;
    }

    @Override
    public String requestVideoCreation(String prompt, String style) {
        return blotatoVideoService.createVideoCreation(prompt, style);
    }

    @Override
    public Optional<String> fetchVideoUrl(String creationId) {
        return Optional.ofNullable(blotatoVideoService.checkVideoStatus(creationId));
    }

    @Override
    public String generateVideo(String prompt, String style) {
        return blotatoVideoService.generateVideo(prompt, style);
    }
}
