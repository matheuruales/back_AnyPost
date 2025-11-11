package com.announcements.AutomateAnnouncements.services;

import com.announcements.AutomateAnnouncements.dtos.request.UserPostRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.PostPublicationResponseDTO;
import com.announcements.AutomateAnnouncements.dtos.response.UserPostResponseDTO;
import com.announcements.AutomateAnnouncements.entities.UserPost;
import com.announcements.AutomateAnnouncements.entities.UserPostPublication;
import com.announcements.AutomateAnnouncements.entities.UserProfile;
import com.announcements.AutomateAnnouncements.repositories.UserPostRepository;
import com.announcements.AutomateAnnouncements.repositories.UserProfileRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserPostService {

    @Autowired
    private UserPostRepository userPostRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Transactional(readOnly = true)
    public List<UserPostResponseDTO> getPostsForUser(String authUserId) {
        return userPostRepository.findByOwnerAuthUserIdOrderByCreatedAtDesc(authUserId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserPostResponseDTO createPost(String authUserId, UserPostRequestDTO dto) {
        UserProfile owner = userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User profile not found for the provided auth user id"));

        UserPost post = new UserPost();
        post.setOwner(owner);
        applyRequestData(post, dto);

        UserPost saved = userPostRepository.save(post);
        return toResponse(saved);
    }

    @Transactional
    public boolean deletePost(UUID postId) {
        return userPostRepository.findById(postId).map(post -> {
            userPostRepository.delete(post);
            return true;
        }).orElse(false);
    }

    private void applyRequestData(UserPost post, UserPostRequestDTO dto) {
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setStatus(dto.getStatus() != null ? dto.getStatus() : (post.getStatus() != null ? post.getStatus() : "draft"));
        post.setVideoUrl(dto.getVideoUrl());
        post.setPublishedAt(dto.getPublishedAt());
        post.setTags(sanitizeList(dto.getTags()));
        post.setTargetPlatforms(sanitizeList(dto.getTargetPlatforms()));

        post.getPublications().clear();
        if (dto.getPublications() != null) {
            dto.getPublications().forEach(pubDto -> {
                UserPostPublication publication = new UserPostPublication();
                publication.setPost(post);
                publication.setNetwork(pubDto.getNetwork());
                publication.setStatus(pubDto.getStatus());
                publication.setPublishedUrl(pubDto.getPublishedUrl());
                publication.setPublishedAt(pubDto.getPublishedAt());
                post.getPublications().add(publication);
            });
        }
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }

        return values.stream()
                .map(value -> value != null ? value.trim() : null)
                .filter(value -> value != null && !value.isEmpty())
                .collect(Collectors.toList());
    }

    private UserPostResponseDTO toResponse(UserPost post) {
        UserPostResponseDTO response = new UserPostResponseDTO();
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setContent(post.getContent());
        response.setStatus(post.getStatus());
        response.setVideoUrl(post.getVideoUrl());
        response.setCreatedAt(post.getCreatedAt());
        response.setUpdatedAt(post.getUpdatedAt());
        response.setPublishedAt(post.getPublishedAt());
        response.setOwnerAuthUserId(post.getOwner() != null ? post.getOwner().getAuthUserId() : null);
        response.setTags(post.getTags() != null ? new ArrayList<>(post.getTags()) : new ArrayList<>());
        response.setTargetPlatforms(post.getTargetPlatforms() != null ? new ArrayList<>(post.getTargetPlatforms()) : new ArrayList<>());
        response.setPublications(post.getPublications() != null
                ? post.getPublications().stream()
                        .map(this::toPublicationResponse)
                        .collect(Collectors.toList())
                : new ArrayList<>());
        return response;
    }

    private PostPublicationResponseDTO toPublicationResponse(UserPostPublication publication) {
        PostPublicationResponseDTO response = new PostPublicationResponseDTO();
        response.setId(publication.getId());
        response.setNetwork(publication.getNetwork());
        response.setStatus(publication.getStatus());
        response.setPublishedUrl(publication.getPublishedUrl());
        response.setPublishedAt(publication.getPublishedAt());
        return response;
    }
}
