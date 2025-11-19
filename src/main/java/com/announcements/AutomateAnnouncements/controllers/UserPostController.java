package com.announcements.AutomateAnnouncements.controllers;

import com.announcements.AutomateAnnouncements.dtos.request.UserPostRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.UserPostResponseDTO;
import com.announcements.AutomateAnnouncements.entities.UserPost;
import com.announcements.AutomateAnnouncements.entities.UserProfile;
import com.announcements.AutomateAnnouncements.security.AuthenticatedUserService;
import com.announcements.AutomateAnnouncements.services.UserPostService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class UserPostController {

    @Autowired
    private UserPostService userPostService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @GetMapping("/users/{authUserId}/posts")
    public List<UserPostResponseDTO> getPostsForUser(@PathVariable String authUserId) {
        UserProfile currentUser = authenticatedUserService.getCurrentUser();
        if (!currentUser.getAuthUserId().equals(authUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot list posts for another user");
        }
        return userPostService.getPostsForUser(currentUser);
    }

    @GetMapping("/user-posts")
    public List<UserPostResponseDTO> findPosts(
            @RequestParam(required = false) String authUserId,
            @RequestParam(required = false) Integer profileId,
            @RequestParam(required = false) String email) {
        UserProfile currentUser = authenticatedUserService.getCurrentUser();
        if (authUserId != null && !authUserId.equals(currentUser.getAuthUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "authUserId does not match current user");
        }
        if (profileId != null && !profileId.equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "profileId does not match current user");
        }
        if (email != null && !email.equalsIgnoreCase(currentUser.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "email does not match current user");
        }
        return userPostService.getPostsForUser(currentUser);
    }

    @GetMapping("/user-posts/{postId}")
    public ResponseEntity<UserPostResponseDTO> getPostById(@PathVariable UUID postId) {
        UserProfile currentUser = authenticatedUserService.getCurrentUser();
        return userPostService.getPostForUser(postId, currentUser)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/user-posts/all")
    public ResponseEntity<?> getAllPosts() {
        if (!authenticatedUserService.currentUserHasRole("ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role is required");
        }
        List<UserPost> allPosts = userPostService.getAllPostsForDebug();
        return ResponseEntity.ok(allPosts.stream()
                .map(post -> {
                    var map = new java.util.HashMap<String, Object>();
                    map.put("id", post.getId());
                    map.put("title", post.getTitle());
                    map.put("status", post.getStatus());
                    map.put("ownerAuthUserId", post.getOwner() != null ? post.getOwner().getAuthUserId() : null);
                    map.put("ownerEmail", post.getOwner() != null ? post.getOwner().getEmail() : null);
                    map.put("createdAt", post.getCreatedAt());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList()));
    }

    @PostMapping("/users/{authUserId}/posts")
    public ResponseEntity<UserPostResponseDTO> createPost(@PathVariable String authUserId,
            @RequestBody @Valid UserPostRequestDTO dto) {
        UserProfile currentUser = authenticatedUserService.getCurrentUser();
        if (!currentUser.getAuthUserId().equals(authUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot create posts for another user");
        }
        UserPostResponseDTO created = userPostService.createPost(currentUser, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable UUID postId) {
        UserProfile currentUser = authenticatedUserService.getCurrentUser();
        if (userPostService.deletePost(postId, currentUser)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Public endpoint to view a shared post.
     * No authentication required. Only returns published posts.
     */
    @GetMapping("/posts/public/{postId}")
    public ResponseEntity<UserPostResponseDTO> getPublicPost(@PathVariable UUID postId) {
        return userPostService.getPublicPost(postId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
