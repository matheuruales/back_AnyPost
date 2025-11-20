package com.announcements.AutomateAnnouncements.services;

import com.announcements.AutomateAnnouncements.dtos.request.AiImagePublishRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.request.AssetRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.request.PostDraftRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.request.UserPostRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.AssetResponseDTO;
import com.announcements.AutomateAnnouncements.dtos.response.PostDraftResponseDTO;
import com.announcements.AutomateAnnouncements.entities.UserProfile;
import com.announcements.AutomateAnnouncements.integration.N8nIntegrationService;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class AiImagePublicationService {

    private final AssetService assetService;
    private final PostDraftService postDraftService;
    private final UserPostService userPostService;
    private final N8nIntegrationService n8nIntegrationService;
    private final TargetAudienceTranslator targetAudienceTranslator;
    private final String containerName;

    public AiImagePublicationService(AssetService assetService,
                                     PostDraftService postDraftService,
                                     UserPostService userPostService,
                                     N8nIntegrationService n8nIntegrationService,
                                     TargetAudienceTranslator targetAudienceTranslator,
                                     @Value("${azure.blob.container-name}") String containerName) {
        this.assetService = assetService;
        this.postDraftService = postDraftService;
        this.userPostService = userPostService;
        this.n8nIntegrationService = n8nIntegrationService;
        this.targetAudienceTranslator = targetAudienceTranslator;
        this.containerName = containerName;
    }

    /**
     * Publica una imagen previamente generada por IA usando la URL de Blob ya existente.
     * No vuelve a subir el archivo; solo crea los registros internos y envía el mismo JSON a n8n.
     */
    @Transactional
    public String publish(UserProfile owner, AiImagePublishRequestDTO request) {
        String blobUrl = request.getBlobUrl();
        validateBlobUrl(blobUrl);

        log.info("Publishing AI-generated image for user {} to targets {}", owner.getId(), request.getTargets());

        AssetRequestDTO assetRequest = new AssetRequestDTO();
        assetRequest.setOwner(owner.getId());
        assetRequest.setType("image");
        assetRequest.setSource("ai-generated");
        assetRequest.setBlobUrl(blobUrl);
        AssetResponseDTO asset = assetService.create(assetRequest);

        PostDraftRequestDTO postDraftRequest = new PostDraftRequestDTO();
        postDraftRequest.setTitle(request.getTitle());
        postDraftRequest.setDescription(request.getDescription());
        postDraftRequest.setAssetId(asset.getId());
        postDraftRequest.setTargets(request.getTargets());
        postDraftRequest.setStatus("pending");
        PostDraftResponseDTO postDraft = postDraftService.create(postDraftRequest);

        UserPostRequestDTO postRequest = new UserPostRequestDTO();
        postRequest.setTitle(request.getTitle());
        postRequest.setContent(request.getDescription());
        postRequest.setStatus("published");
        postRequest.setImageUrl(blobUrl);
        postRequest.setTargetPlatforms(targetAudienceTranslator.toAudienceList(request.getTargets()));
        userPostService.createPost(owner, postRequest);

        n8nIntegrationService.sendMediaToN8n(
                request.getTitle(), request.getDescription(), blobUrl, postDraft.getTargets());

        return blobUrl;
    }

    private void validateBlobUrl(String blobUrl) {
        if (!StringUtils.hasText(blobUrl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "blobUrl es obligatorio");
        }

        try {
            URI uri = new URI(blobUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se permiten URLs HTTPS");
            }
            if (uri.getHost() == null || !uri.getHost().endsWith(".blob.core.windows.net")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La URL debe apuntar a Azure Blob Storage");
            }
            if (StringUtils.hasText(containerName)) {
                String expectedPrefix = "/" + containerName + "/";
                if (uri.getPath() == null || !uri.getPath().startsWith(expectedPrefix)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La URL no pertenece al contenedor configurado");
                }
            }
        } catch (URISyntaxException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La URL del blob no es válida");
        }
    }
}
