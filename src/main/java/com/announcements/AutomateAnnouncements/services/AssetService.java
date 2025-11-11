package com.announcements.AutomateAnnouncements.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.announcements.AutomateAnnouncements.dtos.request.AssetRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.AssetResponseDTO;
import com.announcements.AutomateAnnouncements.entities.Asset;
import com.announcements.AutomateAnnouncements.repositories.AssetRepository;

@Service
public class AssetService {

    @Autowired
    private AssetRepository assetRepository;

    public List<AssetResponseDTO> getAll() {
        return assetRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public AssetResponseDTO create(AssetRequestDTO dto) {
        Asset asset = new Asset();
        asset.setOwner(dto.getOwner());
        asset.setType(dto.getType());
        asset.setSource(dto.getSource());
        asset.setBlobUrl(dto.getBlobUrl());
        asset.setCreatedAt(LocalDateTime.now());
        Asset saved = assetRepository.save(asset);
        return toResponse(saved);
    }

    public Optional<AssetResponseDTO> getById(Integer id) {
        return assetRepository.findById(id).map(this::toResponse);
    }

    public boolean delete(Integer id) {
        if (assetRepository.existsById(id)) {
            assetRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private AssetResponseDTO toResponse(Asset a) {
        AssetResponseDTO r = new AssetResponseDTO();
        r.setId(a.getId());
        r.setOwner(a.getOwner());
        r.setType(a.getType());
        r.setSource(a.getSource());
        r.setBlobUrl(a.getBlobUrl());
        r.setCreatedAt(a.getCreatedAt());
        return r;
    }
}
