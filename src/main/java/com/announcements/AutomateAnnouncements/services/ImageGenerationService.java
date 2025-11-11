package com.announcements.AutomateAnnouncements.services;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import com.announcements.AutomateAnnouncements.dtos.request.ImageGenerationRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.ImageGenerationResponseDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ImageGenerationService {

    private final WebClient webClient;
    private final String defaultModel;

    public ImageGenerationService(
            WebClient.Builder webClientBuilder,
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.api.base-url:https://api.openai.com/v1}") String apiBaseUrl,
            @Value("${openai.images.model:dall-e-3}") String defaultModel) {

        String sanitizedApiKey = sanitizeConfigValue(apiKey);
        if (!StringUtils.hasText(sanitizedApiKey)) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }

        String sanitizedBaseUrl = sanitizeConfigValue(apiBaseUrl);
        if (!StringUtils.hasText(sanitizedBaseUrl)) {
            sanitizedBaseUrl = "https://api.openai.com/v1";
        }
        String normalizedBaseUrl = sanitizedBaseUrl.endsWith("/")
                ? sanitizedBaseUrl.substring(0, sanitizedBaseUrl.length() - 1)
                : sanitizedBaseUrl;

        String sanitizedModel = sanitizeConfigValue(defaultModel);
        this.defaultModel = StringUtils.hasText(sanitizedModel) ? sanitizedModel : "dall-e-3";

        this.webClient = webClientBuilder
                .baseUrl(normalizedBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + sanitizedApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public ImageGenerationResponseDTO generateImage(ImageGenerationRequestDTO request) {
        String prompt = request.getPrompt().trim();
        String size = StringUtils.hasText(request.getSize()) ? request.getSize() : "1024x1024";
        String quality = StringUtils.hasText(request.getQuality()) ? request.getQuality() : "standard";
        String style = StringUtils.hasText(request.getStyle()) ? request.getStyle() : "vivid";

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", defaultModel);
        payload.put("prompt", prompt);
        payload.put("n", 1);
        payload.put("size", size);
        payload.put("quality", quality);
        payload.put("style", style);
        payload.put("response_format", "url");

        log.info("Requesting OpenAI image generation with size={}, quality={}, style={}", size, quality, style);

        OpenAiImageResponse response = webClient.post()
                .uri("/images/generations")
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .map(body -> {
                            log.error("OpenAI API error: status={}, body={}", clientResponse.statusCode(), body);
                            return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                                    "El servicio de imágenes no está disponible en este momento.");
                        }))
                .bodyToMono(OpenAiImageResponse.class)
                .onErrorResume(throwable -> {
                    log.error("Unexpected error calling OpenAI image API", throwable);
                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "No se pudo generar la imagen. Intenta de nuevo en unos minutos."));
                })
                .block();

        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "El servicio de imágenes no devolvió resultados.");
        }

        OpenAiImageData imageData = response.getData().get(0);
        String imageUrl = imageData.getUrl();

        if (!StringUtils.hasText(imageUrl) && StringUtils.hasText(imageData.getB64Json())) {
            imageUrl = "data:image/png;base64," + imageData.getB64Json();
            log.warn("OpenAI image API returned base64 data instead of URL. Converting to data URI.");
        }

        if (!StringUtils.hasText(imageUrl)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "El servicio de imágenes no devolvió una URL válida.");
        }

        ImageGenerationResponseDTO dto = new ImageGenerationResponseDTO();
        dto.setPrompt(prompt);
        dto.setRevisedPrompt(imageData.getRevisedPrompt());
        dto.setImageUrl(imageUrl);
        dto.setSize(size);
        dto.setQuality(quality);
        dto.setStyle(style);
        dto.setGeneratedAt(LocalDateTime.now());
        return dto;
    }

    public ImageProxyResult proxyImageFromUrl(String imageUrl) {
        URI uri = validateImageUrl(imageUrl);
        log.info("Proxying AI image from host {}", uri.getHost());

        try {
            return webClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .exchangeToMono(clientResponse -> {
                        if (clientResponse.statusCode().isError()) {
                            return clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("<empty body>")
                                    .flatMap(body -> {
                                        log.error("Failed to download AI image: status={}, body={}",
                                                clientResponse.statusCode(), body);
                                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                                                "No se pudo descargar la imagen generada."));
                                    });
                        }

                        MediaType mediaType = clientResponse.headers().contentType()
                                .orElse(MediaType.IMAGE_PNG);
                        String filename = extractFilename(uri.getPath(), mediaType);

                        return clientResponse.bodyToMono(byte[].class)
                                .map(bytes -> new ImageProxyResult(bytes, mediaType, filename));
                    })
                    .blockOptional()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "El servicio de imágenes no devolvió datos."));
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error downloading AI image from {}", uri, ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo descargar la imagen generada. Intenta nuevamente.");
        }
    }

    private URI validateImageUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La URL de la imagen es obligatoria.");
        }

        try {
            URI uri = new URI(imageUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se permiten URLs HTTPS.");
            }

            String host = uri.getHost();
            if (host == null || !host.endsWith(".blob.core.windows.net")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Solo se permiten URLs de Azure Blob Storage.");
            }

            return uri;
        } catch (URISyntaxException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La URL de la imagen no es válida.");
        }
    }

    private String extractFilename(String path, MediaType mediaType) {
        if (StringUtils.hasText(path)) {
            int lastSlash = path.lastIndexOf('/');
            String candidate = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
            if (StringUtils.hasText(candidate)) {
                return candidate;
            }
        }

        String extension = "png";
        if (MediaType.IMAGE_JPEG.isCompatibleWith(mediaType)) {
            extension = "jpg";
        } else if (MediaType.IMAGE_GIF.isCompatibleWith(mediaType)) {
            extension = "gif";
        } else if (MediaType.valueOf("image/webp").isCompatibleWith(mediaType)) {
            extension = "webp";
        }

        return "generated-" + System.currentTimeMillis() + "." + extension;
    }

    public record ImageProxyResult(byte[] data, MediaType contentType, String filename) {
    }

    @Data
    private static class OpenAiImageResponse {
        private List<OpenAiImageData> data;
    }

    @Data
    private static class OpenAiImageData {
        private String url;
        @JsonProperty("revised_prompt")
        private String revisedPrompt;
        @JsonProperty("b64_json")
        private String b64Json;
    }

    private String sanitizeConfigValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }
}
