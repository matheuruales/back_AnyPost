package com.announcements.AutomateAnnouncements.security;

import com.announcements.AutomateAnnouncements.entities.UserProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final long ONE_HOUR = 3_600_000L;

    @Test
    void generatesTokenEvenWhenSecretIsShort() {
        JwtService jwtService = new JwtService("tiny-secret", ONE_HOUR);
        UserProfile user = buildUser();

        String token = jwtService.generateToken(user);

        assertNotNull(token, "Token should be generated even if the provided secret is short");
        assertEquals(user.getEmail(), jwtService.extractUsername(token));
    }

    @Test
    void throwsHelpfulErrorWhenSecretMissing() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new JwtService("", ONE_HOUR));
        assertEquals(
                "JWT secret is not configured. Set APPLICATION_SECURITY_JWT_SECRET to a random string with at least 32 characters.",
                exception.getMessage());
    }

    private UserProfile buildUser() {
        UserProfile user = new UserProfile();
        user.setEmail("test@example.com");
        user.setDisplayName("Test User");
        user.setAuthUserId("auth-123");
        user.setRole("ROLE_USER");
        return user;
    }
}
