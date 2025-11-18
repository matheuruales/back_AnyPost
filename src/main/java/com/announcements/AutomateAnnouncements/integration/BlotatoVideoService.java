package com.announcements.AutomateAnnouncements.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class BlotatoVideoService {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;
    private final String templateId;
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE =
            new ParameterizedTypeReference<>() {};

    public BlotatoVideoService(@Value("${blotato.api.key}") String apiKey,
                              @Value("${blotato.api.base-url}") String baseUrl,
                              @Value("${blotato.api.template-id}") String templateId) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.templateId = templateId;

        // Configure RestTemplate with sensible timeouts
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000); // 5s connect
        requestFactory.setReadTimeout(30000); // 30s read
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public String generateVideo(String prompt, String style) {
        log.info("Starting video generation with Blotato API for prompt: {}", prompt);

        try {
            // Step 1: Create video creation request
            String creationId = createVideoCreation(prompt, style);
            log.info("Video creation started with ID: {}", creationId);

            // Step 2: Poll for completion
            String videoUrl = waitForVideoCompletion(creationId);
            log.info("Video generation completed. URL: {}", videoUrl);

            return videoUrl;

        } catch (Exception e) {
            log.error("Failed to generate video with Blotato API: {}", e.getMessage());
            throw new RuntimeException("Video generation failed", e);
        }
    }

    public String createVideoCreation(String prompt, String style) {
        String url = baseUrl + "/videos/creations";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        // Estructura correcta del cuerpo según la API de Blotato
        Map<String, Object> requestBody = Map.of(
            "template", Map.of("id", templateId),
            "script", prompt,
            "style", style != null ? style : "cinematic"
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        log.info("Sending creation request to Blotato API: {}", requestBody);

        // Simple retry loop for transient failures
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ResponseEntity<Map<String, Object>> response =
                        restTemplate.exchange(url, HttpMethod.POST, entity, MAP_RESPONSE);

                log.debug("Blotato create response status: {}", response.getStatusCode());
                log.debug("Blotato create response headers: {}", response.getHeaders());
                log.debug("Blotato create response body: {}", response.getBody());

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> body = response.getBody();

                    // Try several locations for creation id
                    Object creationId = null;
                    if (body.get("id") != null) {
                        creationId = body.get("id");
                    } else if (body.get("item") instanceof Map<?, ?> item) {
                        creationId = item.get("id");
                    } else if (body.get("data") instanceof Map<?, ?> data) {
                        creationId = data.get("id");
                    }

                    // Fallback: try Location header (if present)
                    if (creationId == null && response.getHeaders().getLocation() != null) {
                        try {
                            String path = response.getHeaders().getLocation().getPath();
                            if (path != null) {
                                String[] parts = path.split("/");
                                creationId = parts[parts.length - 1];
                            }
                        } catch (Exception ex) {
                            // ignore parsing errors
                        }
                    }

                    if (creationId == null) {
                        throw new RuntimeException("No creation ID received from Blotato API - body: " + body);
                    }
                    return creationId.toString();
                } else {
                    // If 4xx, do not retry; include body in exception
                    if (response.getStatusCode().is4xxClientError()) {
                        throw new RuntimeException("Failed to create video: " + response.getStatusCode() + " - " + response.getBody());
                    }
                }

            } catch (Exception e) {
                log.warn("Attempt {} to create video failed: {}", attempt, e.getMessage());
                if (attempt == maxAttempts) {
                    log.error("Failed to generate video with Blotato API: {}", e.getMessage());
                    throw new RuntimeException("Video generation failed", e);
                }
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        throw new RuntimeException("Failed to create video after retries");
    }

    private String waitForVideoCompletion(String creationId) throws InterruptedException {
        String statusUrl = baseUrl + "/videos/creations/" + creationId;
        int maxAttempts = 60; // 5 minutes with 5 second intervals
        int attempt = 0;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        while (attempt < maxAttempts) {
            try {
                log.debug("Checking video status for creation ID: {} (attempt {})", creationId, attempt + 1);
                ResponseEntity<Map<String, Object>> response =
                        restTemplate.exchange(statusUrl, HttpMethod.GET, entity, MAP_RESPONSE);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> body = response.getBody();

                    // Extract status from root or nested item
                    String status = null;
                    if (body.get("status") instanceof String s) {
                        status = s;
                    } else if (body.get("item") instanceof Map<?, ?> item) {
                        Object innerStatus = item.get("status");
                        if (innerStatus instanceof String s) status = s;
                    }

                    if ("completed".equals(status)) {
                        // Try multiple keys for video URL
                        String videoUrl = null;
                        if (body.get("videoUrl") instanceof String) videoUrl = (String) body.get("videoUrl");
                        else if (body.get("item") instanceof Map<?, ?> item && item.get("videoUrl") instanceof String)
                            videoUrl = (String) item.get("videoUrl");
                        else if (body.get("item") instanceof Map<?, ?> item && item.get("resultUrl") instanceof String)
                            videoUrl = (String) item.get("resultUrl");

                        if (videoUrl != null) {
                            return videoUrl;
                        } else {
                            throw new RuntimeException("Video completed but no URL provided - body: " + body);
                        }
                    } else if ("failed".equals(status)) {
                        throw new RuntimeException("Video generation failed on Blotato side");
                    }
                    // If still processing, continue polling
                } else {
                    log.debug("Polling response status: {} body: {}", response.getStatusCode(), response.getBody());
                }

            } catch (Exception e) {
                log.warn("Error checking video status: {}", e.getMessage());
            }

            // Wait 5 seconds before next check
            TimeUnit.SECONDS.sleep(5);
            attempt++;
        }

        throw new RuntimeException("Video generation timed out after " + (maxAttempts * 5) + " seconds");
    }

    public String checkVideoStatus(String creationId) {
        String statusUrl = baseUrl + "/videos/creations/" + creationId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response =
                    restTemplate.exchange(statusUrl, HttpMethod.GET, entity, MAP_RESPONSE);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String status = extractStatus(body);

                if ("completed".equals(status)) {
                    String videoUrl = extractVideoUrl(body);
                    if (videoUrl != null) {
                        return videoUrl;
                    }
                } else if ("failed".equals(status)) {
                    throw new RuntimeException("Video generation failed on Blotato side");
                }
            }
        } catch (Exception e) {
            log.warn("Error checking video status for {}: {}", creationId, e.getMessage());
        }

        return null; // Not ready yet or error
    }

    private String extractStatus(Map<String, Object> body) {
        if (body.get("status") instanceof String status) {
            return status;
        }
        if (body.get("item") instanceof Map<?, ?> item && item.get("status") instanceof String status) {
            return status;
        }
        return null;
    }

    private String extractVideoUrl(Map<String, Object> body) {
        if (body.get("videoUrl") instanceof String url) {
            return url;
        }
        if (body.get("item") instanceof Map<?, ?> item) {
            Object directUrl = item.get("videoUrl");
            if (directUrl instanceof String url) {
                return url;
            }
            Object resultUrl = item.get("resultUrl");
            if (resultUrl instanceof String url) {
                return url;
            }
        }
        return null;
    }
}
