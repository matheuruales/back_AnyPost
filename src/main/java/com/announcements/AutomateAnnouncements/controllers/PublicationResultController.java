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

import com.announcements.AutomateAnnouncements.dtos.request.PublicationResultRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.PublicationResultResponseDTO;
import com.announcements.AutomateAnnouncements.services.PublicationResultService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/publication-results")
public class PublicationResultController {

    @Autowired
    private PublicationResultService publicationResultService;

    @GetMapping
    public List<PublicationResultResponseDTO> getAllPublicationResults() {
        return publicationResultService.getAll();
    }

    @PostMapping
    public ResponseEntity<PublicationResultResponseDTO> createPublicationResult(@RequestBody @Valid PublicationResultRequestDTO dto) {
        PublicationResultResponseDTO created = publicationResultService.create(dto);
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicationResultResponseDTO> getPublicationResultById(@PathVariable Integer id) {
        return publicationResultService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePublicationResult(@PathVariable Integer id) {
        if (publicationResultService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}