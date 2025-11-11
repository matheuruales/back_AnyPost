package com.announcements.AutomateAnnouncements.controllers;

import com.announcements.AutomateAnnouncements.dtos.request.UserPostRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.UserPostResponseDTO;
import com.announcements.AutomateAnnouncements.entities.UserPost;
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

@RestController
@RequestMapping("/api")
public class UserPostController {

    @Autowired
    private UserPostService userPostService;

    @GetMapping("/users/{authUserId}/posts")
    public List<UserPostResponseDTO> getPostsForUser(@PathVariable String authUserId) {
        return userPostService.getPostsForUser(authUserId);
    }

    @GetMapping("/user-posts")
    public List<UserPostResponseDTO> findPosts(
            @RequestParam(required = false) String authUserId,
            @RequestParam(required = false) Integer profileId,
            @RequestParam(required = false) String email) {
        return userPostService.getPosts(authUserId, profileId, email);
    }

    @GetMapping("/user-posts/all")
    public ResponseEntity<?> getAllPosts() {
        try {
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
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/users/{authUserId}/posts")
    public ResponseEntity<UserPostResponseDTO> createPost(@PathVariable String authUserId,
            @RequestBody @Valid UserPostRequestDTO dto) {
        UserPostResponseDTO created = userPostService.createPost(authUserId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable UUID postId) {
        if (userPostService.deletePost(postId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
