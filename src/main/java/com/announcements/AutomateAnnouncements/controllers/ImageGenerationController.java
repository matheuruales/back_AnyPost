package com.announcements.AutomateAnnouncements.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.announcements.AutomateAnnouncements.dtos.request.ImageGenerationRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.ImageGenerationResponseDTO;
import com.announcements.AutomateAnnouncements.services.ImageGenerationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/ai/images")
@Tag(name = "AI Image Generation", description = "Endpoints para generar imágenes con IA")
public class ImageGenerationController {

    @Autowired
    private ImageGenerationService imageGenerationService;

    @PostMapping("/generate")
    @Operation(summary = "Generar imagen", description = "Genera una imagen usando IA a partir de un prompt y parámetros opcionales")
    public ResponseEntity<ImageGenerationResponseDTO> generateImage(
            @Valid @RequestBody ImageGenerationRequestDTO request) {
        ImageGenerationResponseDTO response = imageGenerationService.generateImage(request);
        return ResponseEntity.ok(response);
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

    @PostMapping("/upload-to-blob")
    @Operation(summary = "Subir imagen de IA a Blob Storage", description = "Descarga la imagen desde OpenAI y la sube a Azure Blob Storage, devolviendo la URL pública")
    public ResponseEntity<String> uploadImageToBlobStorage(@RequestParam("imageUrl") String imageUrl) {
        try {
            log.info("Received request to upload image to blob storage: {}", imageUrl);
            String blobUrl = imageGenerationService.uploadAiImageToBlobStorage(imageUrl);
            log.info("Successfully uploaded image, returning blob URL: {}", blobUrl);
            return ResponseEntity.ok(blobUrl);
        } catch (org.springframework.web.server.ResponseStatusException e) {
            log.error("ResponseStatusException while uploading image: status={}, message={}", 
                    e.getStatusCode(), e.getReason(), e);
            return ResponseEntity.status(e.getStatusCode()).body("Error al subir imagen: " + e.getReason());
        } catch (Exception e) {
            log.error("Unexpected error while uploading image", e);
            return ResponseEntity.status(500).body("Error al subir imagen: " + e.getMessage());
        }
    }
}
