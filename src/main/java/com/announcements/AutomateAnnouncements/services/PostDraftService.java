package com.announcements.AutomateAnnouncements.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.announcements.AutomateAnnouncements.dtos.request.PostDraftRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.PostDraftResponseDTO;
import com.announcements.AutomateAnnouncements.entities.PostDraft;
import com.announcements.AutomateAnnouncements.repositories.PostDraftRepository;

@Service
public class PostDraftService {

    @Autowired
    private PostDraftRepository postDraftRepository;

    public List<PostDraftResponseDTO> getAll() {
        return postDraftRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PostDraftResponseDTO create(PostDraftRequestDTO dto) {
        PostDraft p = new PostDraft();
        p.setTitle(dto.getTitle());
        p.setDescription(dto.getDescription());
        p.setAssetId(dto.getAssetId());
        p.setTargets(dto.getTargets());
        p.setStatus(dto.getStatus());
        p.setCreatedAt(LocalDateTime.now());
        PostDraft saved = postDraftRepository.save(p);
        return toResponse(saved);
    }

    public Optional<PostDraftResponseDTO> getById(Integer id) {
        return postDraftRepository.findById(id).map(this::toResponse);
    }

    public boolean delete(Integer id) {
        if (postDraftRepository.existsById(id)) {
            postDraftRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private PostDraftResponseDTO toResponse(PostDraft p) {
        PostDraftResponseDTO r = new PostDraftResponseDTO();
        r.setId(p.getId());
        r.setTitle(p.getTitle());
        r.setDescription(p.getDescription());
        r.setAssetId(p.getAssetId());
        r.setTargets(p.getTargets());
        r.setStatus(p.getStatus());
        r.setCreatedAt(p.getCreatedAt());
        return r;
    }
}
