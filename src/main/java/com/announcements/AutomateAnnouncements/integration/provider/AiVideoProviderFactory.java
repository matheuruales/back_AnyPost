package com.announcements.AutomateAnnouncements.integration.provider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Factory Method pattern – resolves the appropriate {@link AiVideoProvider} implementation
 * based on configuration, keeping provider lookup logic in a single place.
 */
@Component
public class AiVideoProviderFactory {

    private final Map<VideoProviderType, AiVideoProvider> providers;
    private final VideoProviderType defaultProviderType;
    private final VideoProviderType asyncProviderType;

    public AiVideoProviderFactory(
            List<AiVideoProvider> providers,
            @Value("${video.provider.default:BLOTATO}") String defaultProvider,
            @Value("${video.provider.async:BLOTATO}") String asyncProvider) {
        this.providers = providers.stream()
                .collect(Collectors.toMap(AiVideoProvider::getType, Function.identity(), (left, right) -> left));
        this.defaultProviderType = VideoProviderType.valueOf(defaultProvider.toUpperCase());
        this.asyncProviderType = VideoProviderType.valueOf(asyncProvider.toUpperCase());
    }

    public AiVideoProvider getProvider(VideoProviderType type) {
        AiVideoProvider provider = providers.get(type);
        if (provider == null) {
            throw new IllegalArgumentException("No AI video provider registered for type " + type);
        }
        return provider;
    }

    public AiVideoProvider getDefaultProvider() {
        return getProvider(defaultProviderType);
    }

    public AiVideoProvider getAsyncProvider() {
        AiVideoProvider provider = getProvider(asyncProviderType);
        if (!provider.supportsAsyncOperations()) {
            throw new IllegalStateException("Configured async provider does not support async operations: " + asyncProviderType);
        }
        return provider;
    }
}
