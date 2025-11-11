package com.announcements.AutomateAnnouncements.services;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.announcements.AutomateAnnouncements.dtos.request.PublicationResultRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.PublicationResultResponseDTO;
import com.announcements.AutomateAnnouncements.entities.PublicationResult;
import com.announcements.AutomateAnnouncements.repositories.PublicationResultRepository;

@Service
public class PublicationResultService {

    @Autowired
    private PublicationResultRepository publicationResultRepository;

    public List<PublicationResultResponseDTO> getAll() {
        return publicationResultRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PublicationResultResponseDTO create(PublicationResultRequestDTO dto) {
        PublicationResult r = new PublicationResult();
        r.setNetwork(dto.getNetwork());
        r.setStatus(dto.getStatus());
        r.setUrl(dto.getUrl());
        r.setError(dto.getError());
        PublicationResult saved = publicationResultRepository.save(r);
        return toResponse(saved);
    }

    public Optional<PublicationResultResponseDTO> getById(Integer id) {
        return publicationResultRepository.findById(id).map(this::toResponse);
    }

    public boolean delete(Integer id) {
        if (publicationResultRepository.existsById(id)) {
            publicationResultRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private PublicationResultResponseDTO toResponse(PublicationResult p) {
        PublicationResultResponseDTO r = new PublicationResultResponseDTO();
        r.setId(p.getId());
        r.setNetwork(p.getNetwork());
        r.setStatus(p.getStatus());
        r.setUrl(p.getUrl());
        r.setError(p.getError());
        return r;
    }
}
