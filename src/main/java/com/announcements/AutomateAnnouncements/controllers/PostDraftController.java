package com.announcements.AutomateAnnouncements.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.announcements.AutomateAnnouncements.dtos.request.PostDraftRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.PostDraftResponseDTO;
import com.announcements.AutomateAnnouncements.services.PostDraftService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/post-drafts")
public class PostDraftController {

    @Autowired
    private PostDraftService postDraftService;

    @GetMapping
    public List<PostDraftResponseDTO> getAllPostDrafts() {
        return postDraftService.getAll();
    }

    @PostMapping
    public ResponseEntity<PostDraftResponseDTO> createPostDraft(@RequestBody @Valid PostDraftRequestDTO dto) {
        PostDraftResponseDTO created = postDraftService.create(dto);
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDraftResponseDTO> getPostDraftById(@PathVariable Integer id) {
        return postDraftService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePostDraft(@PathVariable Integer id) {
        if (postDraftService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}