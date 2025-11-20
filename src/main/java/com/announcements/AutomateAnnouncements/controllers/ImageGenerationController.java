package com.announcements.AutomateAnnouncements.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.announcements.AutomateAnnouncements.dtos.request.ImageGenerationRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.request.AiImagePublishRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.ImageGenerationResponseDTO;
import com.announcements.AutomateAnnouncements.entities.UserProfile;
import com.announcements.AutomateAnnouncements.security.AuthenticatedUserService;
import com.announcements.AutomateAnnouncements.services.AiImagePublicationService;
import com.announcements.AutomateAnnouncements.services.ImageGenerationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai/images")
@Tag(name = "AI Image Generation", description = "Endpoints para generar imágenes con IA")
public class ImageGenerationController {

    @Autowired
    private ImageGenerationService imageGenerationService;

    @Autowired
    private AiImagePublicationService aiImagePublicationService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @PostMapping("/generate")
    @Operation(summary = "Generar imagen", description = "Genera una imagen usando IA a partir de un prompt y parámetros opcionales")
    public ResponseEntity<ImageGenerationResponseDTO> generateImage(
            @Valid @RequestBody ImageGenerationRequestDTO request) {
        ImageGenerationResponseDTO response = imageGenerationService.generateImage(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload-to-blob")
    @Operation(summary = "Subir imagen generada a Blob", description = "Guarda en Blob Storage una imagen generada por IA sin alterar el flujo de subida manual")
    public ResponseEntity<Map<String, String>> uploadToBlob(@RequestParam("imageUrl") String imageUrl) {
        String blobUrl = imageGenerationService.uploadImageToBlob(imageUrl);
        return ResponseEntity.ok(Map.of("blobUrl", blobUrl));
    }

    @PostMapping("/publish")
    @Operation(summary = "Publicar imagen generada", description = "Publica usando una URL de Blob ya existente (generada por IA) y envía el mismo JSON a n8n")
    public ResponseEntity<Map<String, String>> publishGeneratedImage(
            @Valid @RequestBody AiImagePublishRequestDTO request) {
        UserProfile currentUser = authenticatedUserService.getCurrentUser();
        if (!currentUser.getAuthUserId().equals(request.getAuthUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes publicar para tu propio usuario");
        }

        String blobUrl = aiImagePublicationService.publish(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("blobUrl", blobUrl));
    }

    @GetMapping("/proxy")
    @Operation(summary = "Descargar imagen generada", description = "Proxy seguro para recuperar la imagen generada sin restricciones de CORS")
    public ResponseEntity<byte[]> proxyImage(@RequestParam("imageUrl") String imageUrl) {
        ImageGenerationService.ImageProxyResult result = imageGenerationService.proxyImageFromUrl(imageUrl);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + result.filename() + "\"")
                .contentType(result.contentType())
                .body(result.data());
    }
}
