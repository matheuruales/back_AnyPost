package com.announcements.AutomateAnnouncements.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.announcements.AutomateAnnouncements.dtos.request.UserProfileRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.UserProfileResponseDTO;
import com.announcements.AutomateAnnouncements.entities.UserProfile;
import com.announcements.AutomateAnnouncements.repositories.UserProfileRepository;

@Service
public class UserProfileService {

    @Autowired
    private UserProfileRepository userProfileRepository;

    public List<UserProfileResponseDTO> getAll() {
        return userProfileRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public UserProfileResponseDTO create(UserProfileRequestDTO dto) {
        UserProfile u = new UserProfile();
        u.setEmail(dto.getEmail());
        u.setDisplayName(dto.getDisplayName());
        u.setCreatedAt(LocalDateTime.now());
        UserProfile saved = userProfileRepository.save(u);
        return toResponse(saved);
    }

    public Optional<UserProfileResponseDTO> getById(Integer id) {
        return userProfileRepository.findById(id).map(this::toResponse);
    }

    public boolean delete(Integer id) {
        if (userProfileRepository.existsById(id)) {
            userProfileRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private UserProfileResponseDTO toResponse(UserProfile u) {
        UserProfileResponseDTO r = new UserProfileResponseDTO();
        r.setId(u.getId());
        r.setEmail(u.getEmail());
        r.setDisplayName(u.getDisplayName());
        r.setCreatedAt(u.getCreatedAt());
        return r;
    }
}
