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
import com.announcements.AutomateAnnouncements.integration.BlobStorageService;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ImageGenerationService {

    private final WebClient webClient;
    private final WebClient imageDownloadClient;
    private final String defaultModel;
    private final BlobStorageService blobStorageService;

    public ImageGenerationService(
            WebClient.Builder webClientBuilder,
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.api.base-url:https://api.openai.com/v1}") String apiBaseUrl,
            @Value("${openai.images.model:dall-e-3}") String defaultModel,
            BlobStorageService blobStorageService) {

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

        // WebClient for OpenAI API calls (with base URL and auth)
        this.webClient = webClientBuilder
                .baseUrl(normalizedBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + sanitizedApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        
        // Separate WebClient for downloading images (no base URL, no auth headers)
        // Configure with timeouts and user agent to avoid blocking
        this.imageDownloadClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB buffer
                .build();
        
        this.blobStorageService = blobStorageService;
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
                                .orElse(inferContentTypeFromUrl(uri));
                        String filename = extractFilename(uri, mediaType);

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

    /**
     * Uploads an AI-generated image to Azure Blob Storage.
     * Downloads the image from OpenAI URL and uploads it to Blob Storage.
     */
    public String uploadAiImageToBlobStorage(String openAiImageUrl) {
        log.info("Uploading AI-generated image to Blob Storage from URL: {}", openAiImageUrl);
        
        try {
            // Download image from OpenAI (accept any HTTPS URL, not just Azure Blob)
            URI uri = new URI(openAiImageUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se permiten URLs HTTPS.");
            }

            // Use the image download client (no base URL, no auth headers)
            // Use the full URL string to ensure query parameters are preserved
            log.info("Attempting to download image from: {}", uri);
            ImageProxyResult result = imageDownloadClient.get()
                    .uri(uri.toString()) // Use string to preserve query parameters
                    .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                    .accept(MediaType.ALL) // Accept any content type
                    .exchangeToMono(clientResponse -> {
                        log.info("Response status: {}, headers: {}", 
                                clientResponse.statusCode(), 
                                clientResponse.headers().asHttpHeaders());
                        
                        if (clientResponse.statusCode().isError()) {
                            return clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("<empty body>")
                                    .flatMap(body -> {
                                        log.error("Failed to download AI image: status={}, body={}, url={}",
                                                clientResponse.statusCode(), body, uri);
                                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                                                "No se pudo descargar la imagen generada. Status: " + 
                                                clientResponse.statusCode() + ", URL: " + uri));
                                    });
                        }

                        // Get content type from response headers or infer from URL
                        MediaType mediaType = clientResponse.headers().contentType()
                                .orElse(inferContentTypeFromUrl(uri));
                        
                        String filename = extractFilename(uri, mediaType);
                        log.info("Downloaded image: filename={}, contentType={}", filename, mediaType);

                        return clientResponse.bodyToMono(byte[].class)
                                .map(bytes -> {
                                    log.info("Downloaded {} bytes of image data", bytes.length);
                                    return new ImageProxyResult(bytes, mediaType, filename);
                                });
                    })
                    .blockOptional()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "El servicio de imágenes no devolvió datos."));

            // Upload to Blob Storage
            log.info("Uploading {} bytes to Blob Storage with filename: {}", result.data().length, result.filename());
            String blobUrl = blobStorageService.uploadBytes(
                    result.data(),
                    result.filename(),
                    result.contentType().toString()
            );
            
            log.info("Successfully uploaded AI image to Blob Storage: {}", blobUrl);
            return blobUrl;
        } catch (URISyntaxException ex) {
            log.error("Invalid URL syntax: {}", openAiImageUrl, ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La URL de la imagen no es válida: " + ex.getMessage());
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to upload AI image to Blob Storage", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo subir la imagen a Blob Storage: " + ex.getMessage());
        }
    }
    
    /**
     * Infers content type from URL query parameters or path
     */
    private MediaType inferContentTypeFromUrl(URI uri) {
        log.debug("Inferring content type from URI: {}", uri);
        
        // Check query parameters for content type (OpenAI URLs often have rsct parameter)
        String query = uri.getQuery();
        if (query != null) {
            log.debug("Query string: {}", query);
            // Handle both encoded and unencoded parameters
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("rsct=")) {
                    try {
                        String contentType = java.net.URLDecoder.decode(param.substring(5), "UTF-8");
                        log.info("Found content type in query parameter rsct: {}", contentType);
                        MediaType mediaType = MediaType.parseMediaType(contentType);
                        log.info("Parsed media type: {}", mediaType);
                        return mediaType;
                    } catch (Exception e) {
                        log.warn("Failed to parse content type from query parameter: {}", param, e);
                    }
                }
            }
        }
        
        // Fallback: infer from file extension in path
        String path = uri.getPath();
        if (path != null) {
            path = path.toLowerCase();
            log.debug("Inferring from path: {}", path);
            if (path.endsWith(".png")) {
                log.info("Inferred PNG from path extension");
                return MediaType.IMAGE_PNG;
            } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                log.info("Inferred JPEG from path extension");
                return MediaType.IMAGE_JPEG;
            } else if (path.endsWith(".gif")) {
                log.info("Inferred GIF from path extension");
                return MediaType.IMAGE_GIF;
            } else if (path.endsWith(".webp")) {
                log.info("Inferred WEBP from path extension");
                return MediaType.valueOf("image/webp");
            }
        }
        
        // Default to PNG (OpenAI DALL-E typically returns PNG)
        log.info("Using default content type: image/png");
        return MediaType.IMAGE_PNG;
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

            // Accept both Azure Blob Storage URLs and OpenAI URLs
            String host = uri.getHost();
            if (host == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL inválida.");
            }

            // Allow OpenAI URLs (oaidalleapiprodscus.blob.core.windows.net) and Azure Blob URLs
            if (!host.endsWith(".blob.core.windows.net") && !host.contains("openai")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Solo se permiten URLs de Azure Blob Storage o OpenAI.");
            }

            return uri;
        } catch (URISyntaxException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La URL de la imagen no es válida.");
        }
    }

    private String extractFilename(URI uri, MediaType mediaType) {
        String path = uri.getPath();
        if (StringUtils.hasText(path)) {
            // Remove query parameters from path if any
            int queryIndex = path.indexOf('?');
            if (queryIndex > 0) {
                path = path.substring(0, queryIndex);
            }
            
            int lastSlash = path.lastIndexOf('/');
            String candidate = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
            
            // Check if candidate has a valid extension
            if (StringUtils.hasText(candidate) && candidate.contains(".")) {
                String ext = candidate.substring(candidate.lastIndexOf('.') + 1).toLowerCase();
                // Validate extension matches media type
                if (isValidExtensionForMediaType(ext, mediaType)) {
                    return candidate;
                }
            }
        }

        // Generate filename based on media type
        String extension = getExtensionFromMediaType(mediaType);
        return "generated-" + System.currentTimeMillis() + "." + extension;
    }
    
    private boolean isValidExtensionForMediaType(String ext, MediaType mediaType) {
        ext = ext.toLowerCase();
        if (MediaType.IMAGE_PNG.isCompatibleWith(mediaType)) {
            return "png".equals(ext);
        } else if (MediaType.IMAGE_JPEG.isCompatibleWith(mediaType)) {
            return "jpg".equals(ext) || "jpeg".equals(ext);
        } else if (MediaType.IMAGE_GIF.isCompatibleWith(mediaType)) {
            return "gif".equals(ext);
        } else if (MediaType.valueOf("image/webp").isCompatibleWith(mediaType)) {
            return "webp".equals(ext);
        }
        return false;
    }
    
    private String getExtensionFromMediaType(MediaType mediaType) {
        if (MediaType.IMAGE_JPEG.isCompatibleWith(mediaType)) {
            return "jpg";
        } else if (MediaType.IMAGE_GIF.isCompatibleWith(mediaType)) {
            return "gif";
        } else if (MediaType.valueOf("image/webp").isCompatibleWith(mediaType)) {
            return "webp";
        }
        // Default to PNG
        return "png";
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
