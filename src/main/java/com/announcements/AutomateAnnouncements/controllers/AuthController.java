package com.announcements.AutomateAnnouncements.controllers;

import com.announcements.AutomateAnnouncements.dtos.request.AuthLoginRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.request.AuthRegisterRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.request.ForgotPasswordRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.request.ResetPasswordRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.request.VerifyResetCodeRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.AuthResponseDTO;
import com.announcements.AutomateAnnouncements.security.AuthenticatedUserService;
import com.announcements.AutomateAnnouncements.services.AuthService;
import com.announcements.AutomateAnnouncements.services.PasswordResetService;
import com.announcements.AutomateAnnouncements.services.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticatedUserService authenticatedUserService;
    private final UserProfileService userProfileService;
    private final PasswordResetService passwordResetService;

    @Autowired
    public AuthController(AuthService authService,
                          AuthenticatedUserService authenticatedUserService,
                          UserProfileService userProfileService,
                          PasswordResetService passwordResetService) {
        this.authService = authService;
        this.authenticatedUserService = authenticatedUserService;
        this.userProfileService = userProfileService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody AuthRegisterRequestDTO request) {
        log.info("Received register request for {}", request.getEmail());
        return ResponseEntity.status(201).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthLoginRequestDTO request) {
        log.info("Received login request for {}", request.getEmail());
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponseDTO> me() {
        var user = authenticatedUserService.getCurrentUser();
        return ResponseEntity.ok(AuthResponseDTO.builder()
                .token(null)
                .user(userProfileService.toResponse(user))
                .build());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        log.info("Received forgot password request for {}", request.getEmail());
        passwordResetService.sendResetCode(request.getEmail());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<Void> verifyResetCode(@Valid @RequestBody VerifyResetCodeRequestDTO request) {
        passwordResetService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        passwordResetService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }
}
