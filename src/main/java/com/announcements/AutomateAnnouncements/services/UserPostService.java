package com.announcements.AutomateAnnouncements.services;

import com.announcements.AutomateAnnouncements.dtos.request.UserPostRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.UserPostResponseDTO;
import com.announcements.AutomateAnnouncements.entities.UserPost;
import com.announcements.AutomateAnnouncements.entities.UserProfile;
import com.announcements.AutomateAnnouncements.repositories.UserPostRepository;
import com.announcements.AutomateAnnouncements.repositories.UserProfileRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserPostService {

    private static final Logger log = LoggerFactory.getLogger(UserPostService.class);

    private final UserPostRepository userPostRepository;

    private final UserProfileRepository userProfileRepository;
    private final UserPostComposer userPostComposer;
    private final UserPostLookupStep lookupChain;

    public UserPostService(UserPostRepository userPostRepository,
                           UserProfileRepository userProfileRepository,
                           UserPostComposer userPostComposer) {
        this.userPostRepository = userPostRepository;
        this.userProfileRepository = userProfileRepository;
        this.userPostComposer = userPostComposer;
        this.lookupChain = buildLookupChain();
    }

    @Transactional(readOnly = true)
    public List<UserPostResponseDTO> getPostsForUser(String authUserId) {
        return getPosts(authUserId, null, null);
    }

    @Transactional(readOnly = true)
    public List<UserPostResponseDTO> getPostsForUser(UserProfile owner) {
        return getPosts(owner.getAuthUserId(), owner.getId(), owner.getEmail());
    }

    @Transactional(readOnly = true)
    public List<UserPost> getAllPostsForDebug() {
        return userPostRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<UserPostResponseDTO> getPosts(String authUserId, Integer profileId, String email) {
        log.info("Getting posts for authUserId={}, profileId={}, email={}", authUserId, profileId, email);

        if (!StringUtils.hasText(authUserId) && profileId == null && !StringUtils.hasText(email)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Provide at least one identifier (authUserId, profileId, or email)");
        }

        Set<UserPost> aggregatedPosts = new LinkedHashSet<>();
        UserPostQuery query = new UserPostQuery(authUserId, profileId, email, aggregatedPosts);

        lookupChain.process(query);

        List<UserPost> sortedPosts = aggregatedPosts.stream()
                .sorted(Comparator.comparing(
                        UserPost::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        log.info("Returning {} posts after aggregation", sortedPosts.size());
        return sortedPosts.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public UserPostResponseDTO createPost(UserProfile owner, UserPostRequestDTO dto) {
        if (owner == null || owner.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner profile is required");
        }

        UserPost post = userPostComposer.composeNew(owner, dto);

        UserPost saved = userPostRepository.save(post);
        return toResponse(saved);
    }

    @Transactional
    public boolean deletePost(UUID postId, UserProfile owner) {
        return userPostRepository.findById(postId)
                .filter(post -> post.getOwner() != null && post.getOwner().getId().equals(owner.getId()))
                .map(post -> {
                    userPostRepository.delete(post);
                    return true;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Optional<UserPostResponseDTO> getPostForUser(UUID postId, UserProfile owner) {
        return userPostRepository.findById(postId)
                .filter(post -> post.getOwner() != null && post.getOwner().getId().equals(owner.getId()))
                .map(this::toResponse);
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
                        .map(userPostComposer::toPublicationResponse)
                        .collect(Collectors.toList())
                : new ArrayList<>());
        return response;
    }

    private UserPostLookupStep buildLookupChain() {
        UserPostLookupStep authStep = new AuthUserPostsStep(userPostRepository);
        UserPostLookupStep profileStep = authStep.linkNext(new ProfilePostsStep(userPostRepository));
        UserPostLookupStep emailStep = profileStep.linkNext(new EmailPostsStep(userPostRepository));
        emailStep.linkNext(new ProfileExistenceValidationStep(userProfileRepository));
        return authStep;
    }

    private static final class UserPostQuery {
        private final String authUserId;
        private final Integer profileId;
        private final String email;
        private final Set<UserPost> aggregatedPosts;

        private UserPostQuery(String authUserId, Integer profileId, String email, Set<UserPost> aggregatedPosts) {
            this.authUserId = authUserId;
            this.profileId = profileId;
            this.email = email;
            this.aggregatedPosts = aggregatedPosts;
        }

        private String getAuthUserId() {
            return authUserId;
        }

        private Integer getProfileId() {
            return profileId;
        }

        private String getEmail() {
            return email;
        }

        private Set<UserPost> getAggregatedPosts() {
            return aggregatedPosts;
        }
    }

    private abstract static class UserPostLookupStep {
        private UserPostLookupStep next;

        public final void process(UserPostQuery query) {
            handle(query);
            if (next != null) {
                next.process(query);
            }
        }

        public UserPostLookupStep linkNext(UserPostLookupStep nextStep) {
            this.next = nextStep;
            return nextStep;
        }

        protected abstract void handle(UserPostQuery query);
    }

    private static final class AuthUserPostsStep extends UserPostLookupStep {
        private final UserPostRepository repository;

        private AuthUserPostsStep(UserPostRepository repository) {
            this.repository = repository;
        }

        @Override
        protected void handle(UserPostQuery query) {
            if (!StringUtils.hasText(query.getAuthUserId())) {
                return;
            }
            List<UserPost> postsByAuth = repository.findByOwnerAuthUserIdOrderByCreatedAtDesc(query.getAuthUserId());
            query.getAggregatedPosts().addAll(postsByAuth);
            log.info("Found {} posts by authUserId", postsByAuth.size());
        }
    }

    private static final class ProfilePostsStep extends UserPostLookupStep {
        private final UserPostRepository repository;

        private ProfilePostsStep(UserPostRepository repository) {
            this.repository = repository;
        }

        @Override
        protected void handle(UserPostQuery query) {
            if (query.getProfileId() == null) {
                return;
            }
            List<UserPost> postsByProfile = repository.findByOwnerIdOrderByCreatedAtDesc(query.getProfileId());
            query.getAggregatedPosts().addAll(postsByProfile);
            log.info("Found {} posts by profileId", postsByProfile.size());
        }
    }

    private static final class EmailPostsStep extends UserPostLookupStep {
        private final UserPostRepository repository;

        private EmailPostsStep(UserPostRepository repository) {
            this.repository = repository;
        }

        @Override
        protected void handle(UserPostQuery query) {
            if (!StringUtils.hasText(query.getEmail())) {
                return;
            }
            List<UserPost> postsByEmail = repository.findByOwnerEmailIgnoreCaseOrderByCreatedAtDesc(query.getEmail());
            query.getAggregatedPosts().addAll(postsByEmail);
            log.info("Found {} posts by email", postsByEmail.size());
        }
    }

    private final class ProfileExistenceValidationStep extends UserPostLookupStep {
        private final UserProfileRepository repository;

        private ProfileExistenceValidationStep(UserProfileRepository repository) {
            this.repository = repository;
        }

        @Override
        protected void handle(UserPostQuery query) {
            if (!query.getAggregatedPosts().isEmpty()) {
                return;
            }

            log.warn("No posts found, checking if profile exists");
            boolean profileExists = profileExists(query.getAuthUserId(), query.getProfileId(), query.getEmail());
            if (!profileExists) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User profile not found for the provided identifier(s)");
            }
        }

        private boolean profileExists(String authUserId, Integer profileId, String email) {
            if (StringUtils.hasText(authUserId) && repository.findByAuthUserId(authUserId).isPresent()) {
                return true;
            }
            if (profileId != null && repository.existsById(profileId)) {
                return true;
            }
            if (StringUtils.hasText(email)) {
                return repository.findByEmailIgnoreCase(email).isPresent();
            }
            return false;
        }
    }
}
