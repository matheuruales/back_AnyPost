package com.announcements.AutomateAnnouncements.services;

import com.announcements.AutomateAnnouncements.dtos.request.AuthLoginRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.request.AuthRegisterRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.AuthResponseDTO;
import com.announcements.AutomateAnnouncements.dtos.response.UserProfileResponseDTO;
import com.announcements.AutomateAnnouncements.entities.UserProfile;
import com.announcements.AutomateAnnouncements.repositories.UserProfileRepository;
import com.announcements.AutomateAnnouncements.security.JwtService;
import com.announcements.AutomateAnnouncements.security.UserProfileDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class AuthService {

    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserProfileService userProfileService;

    @Autowired
    public AuthService(UserProfileRepository userProfileRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       UserProfileService userProfileService) {
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userProfileService = userProfileService;
    }

    @Transactional
    public AuthResponseDTO register(AuthRegisterRequestDTO request) {
        Optional<UserProfile> existing = userProfileRepository.findByEmailIgnoreCase(request.getEmail());
        if (existing.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        UserProfile profile = new UserProfile();
        profile.setEmail(request.getEmail().trim().toLowerCase());
        profile.setDisplayName(request.getDisplayName());
        profile.setAuthUserId(UUID.randomUUID().toString());
        profile.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        profile.setRole("ROLE_USER");

        UserProfile saved = userProfileRepository.save(profile);
        log.info("Registered new user {}", saved.getEmail());
        String token = jwtService.generateToken(saved);

        return AuthResponseDTO.builder()
                .token(token)
                .user(userProfileService.toResponse(saved))
                .build();
    }

    public AuthResponseDTO login(AuthLoginRequestDTO request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        UserProfileDetails userDetails = (UserProfileDetails) authentication.getPrincipal();
        UserProfile profile = userDetails.getUserProfile();
        String token = jwtService.generateToken(profile);

        return AuthResponseDTO.builder()
                .token(token)
                .user(userProfileService.toResponse(profile))
                .build();
    }
}
