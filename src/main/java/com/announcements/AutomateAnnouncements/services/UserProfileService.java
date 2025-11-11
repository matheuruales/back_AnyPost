package com.announcements.AutomateAnnouncements.services;

import com.announcements.AutomateAnnouncements.dtos.request.UserProfileRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.UserProfileResponseDTO;
import com.announcements.AutomateAnnouncements.entities.UserProfile;
import com.announcements.AutomateAnnouncements.repositories.UserProfileRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

    @Autowired
    private UserProfileRepository userProfileRepository;

    public List<UserProfileResponseDTO> getAll() {
        return userProfileRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public UserProfileResponseDTO create(UserProfileRequestDTO dto) {
        UserProfile profile = userProfileRepository.findByAuthUserId(dto.getAuthUserId())
                .orElseGet(() -> userProfileRepository.findByEmail(dto.getEmail()).orElse(new UserProfile()));

        profile.setEmail(dto.getEmail());
        profile.setDisplayName(dto.getDisplayName());
        profile.setAuthUserId(dto.getAuthUserId());

        UserProfile saved = userProfileRepository.save(profile);
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
        r.setUpdatedAt(u.getUpdatedAt());
        r.setAuthUserId(u.getAuthUserId());
        return r;
    }
}
