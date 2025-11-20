package com.announcements.AutomateAnnouncements.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.announcements.AutomateAnnouncements.dtos.request.UserPostRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.PostPublicationResponseDTO;
import com.announcements.AutomateAnnouncements.entities.UserPost;
import com.announcements.AutomateAnnouncements.entities.UserPostPublication;
import com.announcements.AutomateAnnouncements.entities.UserProfile;

/**
 * Assembles {@link UserPost} instances consistently from incoming requests,
 * centralizing defaults and cleansing rules for content and publications.
 */
@Component
public class UserPostComposer {

    private final TargetAudienceTranslator targetAudienceTranslator;

    public UserPostComposer(TargetAudienceTranslator targetAudienceTranslator) {
        this.targetAudienceTranslator = targetAudienceTranslator;
    }

    public UserPost composeNew(UserProfile owner, UserPostRequestDTO dto) {
        UserPost post = new UserPost();
        post.setOwner(owner);
        applyData(post, dto);
        return post;
    }

    public void refresh(UserPost post, UserPostRequestDTO dto) {
        applyData(post, dto);
    }

    public PostPublicationResponseDTO toPublicationResponse(UserPostPublication publication) {
        PostPublicationResponseDTO response = new PostPublicationResponseDTO();
        response.setId(publication.getId());
        response.setNetwork(publication.getNetwork());
        response.setStatus(publication.getStatus());
        response.setPublishedUrl(publication.getPublishedUrl());
        response.setPublishedAt(publication.getPublishedAt());
        return response;
    }

    private void applyData(UserPost post, UserPostRequestDTO dto) {
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setStatus(resolveStatus(dto.getStatus(), post.getStatus()));
        post.setVideoUrl(dto.getVideoUrl());

        LocalDateTime publishedAt = dto.getPublishedAt() != null
                ? dto.getPublishedAt()
                : (post.getPublishedAt() != null ? post.getPublishedAt() : LocalDateTime.now());
        post.setPublishedAt(publishedAt);

        post.setTags(targetAudienceTranslator.sanitizeAudienceList(dto.getTags()));
        post.setTargetPlatforms(targetAudienceTranslator.sanitizeAudienceList(dto.getTargetPlatforms()));

        post.getPublications().clear();
        if (dto.getPublications() != null) {
            dto.getPublications().forEach(pubDto -> {
                UserPostPublication publication = new UserPostPublication();
                publication.setPost(post);
                publication.setNetwork(pubDto.getNetwork());
                publication.setStatus(resolveStatus(pubDto.getStatus(), "published"));
                publication.setPublishedUrl(pubDto.getPublishedUrl());
                publication.setPublishedAt(
                        pubDto.getPublishedAt() != null ? pubDto.getPublishedAt() : LocalDateTime.now());
                post.getPublications().add(publication);
            });
        }
    }

    private String resolveStatus(String requestedStatus, String fallbackStatus) {
        if (StringUtils.hasText(requestedStatus)) {
            return requestedStatus;
        }
        if (StringUtils.hasText(fallbackStatus)) {
            return fallbackStatus;
        }
        return "published";
    }
}
