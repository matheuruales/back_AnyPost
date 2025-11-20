package com.announcements.AutomateAnnouncements.services;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.announcements.AutomateAnnouncements.dtos.request.PublicationJobRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.PublicationJobResponseDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * Wraps publication job operations to add audit-style tracing without changing core persistence logic.
 */
@Slf4j
@Service
@Primary
public class PublicationJobAuditTrail implements PublicationJobOperations {

    private final PublicationJobService delegate;

    public PublicationJobAuditTrail(PublicationJobService delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<PublicationJobResponseDTO> getAll() {
        List<PublicationJobResponseDTO> jobs = delegate.getAll();
        log.debug("Retrieved {} publication jobs for monitoring", jobs.size());
        return jobs;
    }

    @Override
    public PublicationJobResponseDTO create(PublicationJobRequestDTO dto) {
        PublicationJobResponseDTO created = delegate.create(dto);
        log.info("Created publication job {} for draft {}", created.getId(), created.getPostDraftId());
        return created;
    }

    @Override
    public Optional<PublicationJobResponseDTO> getById(Integer id) {
        Optional<PublicationJobResponseDTO> job = delegate.getById(id);
        job.ifPresent(value -> log.debug("Fetched publication job {} with status {}", value.getId(), value.getStatus()));
        return job;
    }

    @Override
    public boolean delete(Integer id) {
        boolean removed = delegate.delete(id);
        if (removed) {
            log.info("Deleted publication job {}", id);
        } else {
            log.warn("Attempted to delete publication job {} but it was not found", id);
        }
        return removed;
    }
}
