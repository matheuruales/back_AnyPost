package com.announcements.AutomateAnnouncements.integration;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import com.azure.storage.blob.models.PublicAccessType;

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
            BlobContainerClient containerClient = getOrCreateContainerClient();
            String blobName = buildBlobName(file.getOriginalFilename());
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            blobClient.upload(file.getInputStream(), file.getSize(), true);

            return blobClient.getBlobUrl();
        } catch (IOException e) {
            log.error("Failed to upload file to Blob Storage", e);
            throw new RuntimeException("Failed to upload file to Blob Storage", e);
        }
    }

    public String uploadBytes(byte[] data, String originalFilename) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data to upload cannot be empty");
        }

        try {
            BlobContainerClient containerClient = getOrCreateContainerClient();
            String blobName = buildBlobName(originalFilename);
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            try (InputStream inputStream = new ByteArrayInputStream(data)) {
                blobClient.upload(inputStream, data.length, true);
            }

            return blobClient.getBlobUrl();
        } catch (Exception e) {
            log.error("Failed to upload bytes to Blob Storage", e);
            throw new RuntimeException("Failed to upload bytes to Blob Storage", e);
        }
    }

    private BlobContainerClient getOrCreateContainerClient() {
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();

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
        return containerClient;
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
