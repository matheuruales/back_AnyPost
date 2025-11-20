package com.announcements.AutomateAnnouncements.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.announcements.AutomateAnnouncements.services.TargetAudienceTranslator;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class N8nIntegrationService {

    private final WebClient webClient;
    private final String webhookUrl;
    private final TargetAudienceTranslator targetAudienceTranslator;

    public N8nIntegrationService(@Value("${n8n.webhook.url}") String webhookUrl,
                                 TargetAudienceTranslator targetAudienceTranslator) {
        this.webhookUrl = webhookUrl;
        this.targetAudienceTranslator = targetAudienceTranslator;
        this.webClient = WebClient.builder().build();
    }

    public void sendVideoToN8n(String title, String description, String videoUrl, String targets) {
        log.info("Sending video data to n8n webhook: {}", webhookUrl);

        List<String> targetsArray = targetAudienceTranslator.toAudienceList(targets);

        Map<String, Object> payload = Map.of(
            "title", title,
            "description", description,
            "blobUrl", videoUrl,
            "targets", targetsArray
        );

        log.info("Payload to send: {}", payload);

        try {
            webClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .block();

            log.info("Video data sent successfully to n8n");
        } catch (Exception e) {
            log.error("Failed to send video data to n8n: {}", e.getMessage());
            throw new RuntimeException("Failed to send data to n8n", e);
        }
    }
}
