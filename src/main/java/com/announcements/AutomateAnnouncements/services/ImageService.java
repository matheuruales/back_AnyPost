package com.announcements.AutomateAnnouncements.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.announcements.AutomateAnnouncements.dtos.response.AssetResponseDTO;
import com.announcements.AutomateAnnouncements.dtos.response.PostDraftResponseDTO;
import com.announcements.AutomateAnnouncements.dtos.response.UserPostResponseDTO;
import com.announcements.AutomateAnnouncements.dtos.request.UserPostRequestDTO;
import com.announcements.AutomateAnnouncements.integration.BlobStorageService;
import com.announcements.AutomateAnnouncements.integration.N8nIntegrationService;
import com.announcements.AutomateAnnouncements.entities.UserProfile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ImageService {

    @Autowired
    private AssetService assetService;

    @Autowired
    private PostDraftService postDraftService;

    @Autowired
    private UserPostService userPostService;

    @Autowired
    private BlobStorageService blobStorageService;

    @Autowired
    private N8nIntegrationService n8nIntegrationService;

    @Transactional
    public String uploadUserImage(UserProfile userProfile, MultipartFile file, String title, String description, String targets) {
        log.info("Starting image upload process for authUserId: {}, targets: {}", userProfile.getAuthUserId(), targets);

        Integer ownerId = userProfile.getId();

        // Upload file to blob storage
        String imageUrl = blobStorageService.uploadFile(file);
        log.info("Image uploaded to blob storage: {}", imageUrl);

        // Create asset record with type "image"
        com.announcements.AutomateAnnouncements.dtos.request.AssetRequestDTO assetRequest = new com.announcements.AutomateAnnouncements.dtos.request.AssetRequestDTO();
        assetRequest.setOwner(ownerId);
        assetRequest.setType("image");
        assetRequest.setSource(file.getOriginalFilename());
        assetRequest.setBlobUrl(imageUrl);
        AssetResponseDTO asset = assetService.create(assetRequest);
        log.info("Asset created with ID: {}", asset.getId());

        // Create post draft (legacy)
        com.announcements.AutomateAnnouncements.dtos.request.PostDraftRequestDTO postDraftRequest = new com.announcements.AutomateAnnouncements.dtos.request.PostDraftRequestDTO();
        postDraftRequest.setTitle(title);
        postDraftRequest.setDescription(description);
        postDraftRequest.setAssetId(asset.getId());
        postDraftRequest.setTargets(targets);
        postDraftRequest.setStatus("pending");
        PostDraftResponseDTO postDraft = postDraftService.create(postDraftRequest);
        log.info("Post draft created with ID: {}", postDraft.getId());

        // Create UserPost (new system)
        UserPostRequestDTO userPostRequest = new UserPostRequestDTO();
        userPostRequest.setTitle(title);
        userPostRequest.setContent(description);
        userPostRequest.setImageUrl(imageUrl);
        userPostRequest.setStatus("published"); // Auto-publish when image is uploaded

        // Parse targets into list
        List<String> targetList = Arrays.stream(targets.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        userPostRequest.setTargetPlatforms(targetList);

        UserPostResponseDTO userPost = userPostService.createPost(userProfile, userPostRequest);
        log.info("UserPost created with ID: {}", userPost.getId());

        // Send to n8n (using same method, it should handle images too)
        n8nIntegrationService.sendVideoToN8n(title, description, imageUrl, postDraft.getTargets());
        log.info("Image data sent to n8n successfully");

        return imageUrl;
    }
}

