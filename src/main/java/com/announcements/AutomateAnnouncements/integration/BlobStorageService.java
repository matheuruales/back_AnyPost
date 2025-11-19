package com.announcements.AutomateAnnouncements.integration;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.azure.storage.blob.models.PublicAccessType;
import java.io.IOException;
import java.util.UUID;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class BlobStorageService {

    @Value("${azure.blob.connection-string}")
    private String connectionString;

    @Value("${azure.blob.container-name}")
    private String containerName;

    @Value("${azure.blob.public:false}")
    private boolean containerPublic;

    public String uploadFile(MultipartFile file) {
        try {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            // Crear el contenedor si no existe y configurar acceso público
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            if (!containerClient.exists()) {
                log.info("Container '{}' does not exist, creating it...", containerName);
                containerClient.create();
                log.info("Container '{}' created successfully", containerName);
            }

            // Asegurar que el contenedor sea público si está configurado así
            if (containerPublic) {
                try {
                    containerClient.setAccessPolicy(PublicAccessType.CONTAINER, null);
                    log.info("Container '{}' access set to PUBLIC (blob/list).", containerName);
                } catch (Exception e) {
                    log.warn("Failed to set container '{}' public access: {}", containerName, e.getMessage());
                }
            }

            String blobName = buildBlobName(file.getOriginalFilename());
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            // Upload file
            blobClient.upload(file.getInputStream(), file.getSize(), true);
            
            // Set content type from file if available
            if (file.getContentType() != null) {
                BlobHttpHeaders headers = new BlobHttpHeaders();
                headers.setContentType(file.getContentType());
                blobClient.setHttpHeaders(headers);
            }

            return blobClient.getBlobUrl();
        } catch (IOException e) {
            log.error("Failed to upload file to Blob Storage", e);
            throw new RuntimeException("Failed to upload file to Blob Storage", e);
        }
    }

    public String uploadBytes(byte[] data, String filename, String contentType) {
        try {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            // Crear el contenedor si no existe y configurar acceso público
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            if (!containerClient.exists()) {
                log.info("Container '{}' does not exist, creating it...", containerName);
                containerClient.create();
                log.info("Container '{}' created successfully", containerName);
            }

            // Asegurar que el contenedor sea público si está configurado así
            if (containerPublic) {
                try {
                    containerClient.setAccessPolicy(PublicAccessType.CONTAINER, null);
                    log.info("Container '{}' access set to PUBLIC (blob/list).", containerName);
                } catch (Exception e) {
                    log.warn("Failed to set container '{}' public access: {}", containerName, e.getMessage());
                }
            }

            String blobName = buildBlobName(filename);
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            // Upload bytes
            blobClient.upload(new java.io.ByteArrayInputStream(data), data.length, true);
            
            // Set content type if provided or inferred
            String contentTypeToSet = contentType;
            if (!StringUtils.hasText(contentTypeToSet)) {
                contentTypeToSet = inferContentTypeFromFilename(filename);
            }
            
            if (StringUtils.hasText(contentTypeToSet)) {
                BlobHttpHeaders headers = new BlobHttpHeaders();
                headers.setContentType(contentTypeToSet);
                blobClient.setHttpHeaders(headers);
            }

            return blobClient.getBlobUrl();
        } catch (Exception e) {
            log.error("Failed to upload bytes to Blob Storage", e);
            throw new RuntimeException("Failed to upload bytes to Blob Storage", e);
        }
    }

    private String buildBlobName(String originalFilename) {
        String sanitizedExtension = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
            sanitizedExtension = "." + ext.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        }
        return UUID.randomUUID().toString() + sanitizedExtension;
    }

    private String inferContentTypeFromFilename(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return null;
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            case "svg" -> "image/svg+xml";
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "avi" -> "video/x-msvideo";
            case "mov" -> "video/quicktime";
            default -> null;
        };
    }
}
