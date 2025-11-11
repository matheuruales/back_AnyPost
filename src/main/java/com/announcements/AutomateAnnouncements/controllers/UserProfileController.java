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

import com.announcements.AutomateAnnouncements.dtos.request.UserProfileRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.UserProfileResponseDTO;
import com.announcements.AutomateAnnouncements.services.UserProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/user-profiles")
public class UserProfileController {

    @Autowired
    private UserProfileService userProfileService;

    @GetMapping
    public List<UserProfileResponseDTO> getAllUserProfiles() {
        return userProfileService.getAll();
    }

    @PostMapping
    public ResponseEntity<UserProfileResponseDTO> createUserProfile(@RequestBody @Valid UserProfileRequestDTO dto) {
        UserProfileResponseDTO created = userProfileService.create(dto);
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponseDTO> getUserProfileById(@PathVariable Integer id) {
        return userProfileService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserProfile(@PathVariable Integer id) {
        if (userProfileService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}