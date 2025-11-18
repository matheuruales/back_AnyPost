package com.announcements.AutomateAnnouncements.integration;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
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

            // Crear el contenedor si no existe
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            if (!containerClient.exists()) {
                log.info("Container '{}' does not exist, creating it...", containerName);
                containerClient.create();
                log.info("Container '{}' created successfully", containerName);

                if (containerPublic) {
                    try {
                        containerClient.setAccessPolicy(PublicAccessType.CONTAINER, null);
                        log.info("Container '{}' access set to PUBLIC (blob/list).", containerName);
                    } catch (Exception e) {
                        log.warn("Failed to set container '{}' public access: {}", containerName, e.getMessage());
                    }
                }
            }

            String blobName = buildBlobName(file.getOriginalFilename());
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            blobClient.upload(file.getInputStream(), file.getSize(), true);

            return blobClient.getBlobUrl();
        } catch (IOException e) {
            log.error("Failed to upload file to Blob Storage", e);
            throw new RuntimeException("Failed to upload file to Blob Storage", e);
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
}
