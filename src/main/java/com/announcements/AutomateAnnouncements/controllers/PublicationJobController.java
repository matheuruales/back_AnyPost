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

import com.announcements.AutomateAnnouncements.dtos.request.PublicationJobRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.PublicationJobResponseDTO;
import com.announcements.AutomateAnnouncements.services.PublicationJobOperations;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/publication-jobs")
public class PublicationJobController {

    private final PublicationJobOperations publicationJobOperations;

    @Autowired
    public PublicationJobController(PublicationJobOperations publicationJobOperations) {
        this.publicationJobOperations = publicationJobOperations;
    }

    @GetMapping
    public List<PublicationJobResponseDTO> getAllPublicationJobs() {
        return publicationJobOperations.getAll();
    }

    @PostMapping
    public ResponseEntity<PublicationJobResponseDTO> createPublicationJob(@RequestBody @Valid PublicationJobRequestDTO dto) {
        PublicationJobResponseDTO created = publicationJobOperations.create(dto);
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicationJobResponseDTO> getPublicationJobById(@PathVariable Integer id) {
        return publicationJobOperations.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePublicationJob(@PathVariable Integer id) {
        if (publicationJobOperations.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
