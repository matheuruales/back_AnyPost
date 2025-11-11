package com.announcements.AutomateAnnouncements.controllers;

import com.announcements.AutomateAnnouncements.dtos.request.UserPostRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.UserPostResponseDTO;
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
