package com.announcements.AutomateAnnouncements.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.announcements.AutomateAnnouncements.dtos.request.PublicationJobRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.PublicationJobResponseDTO;
import com.announcements.AutomateAnnouncements.entities.PublicationJob;
import com.announcements.AutomateAnnouncements.repositories.PublicationJobRepository;

@Service
public class PublicationJobService {

    @Autowired
    private PublicationJobRepository publicationJobRepository;

    public List<PublicationJobResponseDTO> getAll() {
        return publicationJobRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PublicationJobResponseDTO create(PublicationJobRequestDTO dto) {
        PublicationJob j = new PublicationJob();
        j.setPostDraftId(dto.getPostDraftId());
        j.setStatus(dto.getStatus());
        j.setRequestedAt(LocalDateTime.now());
        PublicationJob saved = publicationJobRepository.save(j);
        return toResponse(saved);
    }

    public Optional<PublicationJobResponseDTO> getById(Integer id) {
        return publicationJobRepository.findById(id).map(this::toResponse);
    }

    public boolean delete(Integer id) {
        if (publicationJobRepository.existsById(id)) {
            publicationJobRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private PublicationJobResponseDTO toResponse(PublicationJob j) {
        PublicationJobResponseDTO r = new PublicationJobResponseDTO();
        r.setId(j.getId());
        r.setPostDraftId(j.getPostDraftId());
        r.setStatus(j.getStatus());
        r.setRequestedAt(j.getRequestedAt());
        return r;
    }
}
