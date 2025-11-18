package com.announcements.AutomateAnnouncements.security;

import com.announcements.AutomateAnnouncements.entities.UserProfile;
import com.announcements.AutomateAnnouncements.repositories.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthenticatedUserService {

    private final UserProfileRepository userProfileRepository;

    public AuthenticatedUserService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public UserProfile getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user");
        }

        String email = authentication.getName();
        return userProfileRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public boolean currentUserHasRole(String role) {
        UserProfile user = getCurrentUser();
        return role != null && role.equalsIgnoreCase(user.getRole());
    }
}
