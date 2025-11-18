package com.announcements.AutomateAnnouncements.services;

import com.announcements.AutomateAnnouncements.entities.PasswordResetToken;
import com.announcements.AutomateAnnouncements.entities.UserProfile;
import com.announcements.AutomateAnnouncements.repositories.PasswordResetTokenRepository;
import com.announcements.AutomateAnnouncements.repositories.UserProfileRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserProfileRepository userProfileRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.password-reset.expiration-minutes:15}")
    private long expirationMinutes;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    @Transactional
    public void sendResetCode(String email) {
        UserProfile user = userProfileRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe una cuenta con ese correo"));

        // invalidate previous pending tokens to avoid reuse
        List<PasswordResetToken> activeTokens = passwordResetTokenRepository.findByUserProfileAndUsedFalse(user);
        activeTokens.forEach(token -> token.setUsed(true));
        passwordResetTokenRepository.saveAll(activeTokens);

        String code = generateCode();

        PasswordResetToken token = new PasswordResetToken();
        token.setUserProfile(user);
        token.setCode(code);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(expirationMinutes));
        passwordResetTokenRepository.save(token);

        sendCodeEmail(user.getEmail(), code);
        log.info("Password reset code sent to {}", user.getEmail());
    }

    @Transactional
    public void verifyCode(String email, String code) {
        getValidToken(email, code);
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        PasswordResetToken token = getValidToken(email, code);
        UserProfile user = token.getUserProfile();
        user.setPasswordHash(passwordEncoder.encode(newPassword));

        token.setUsed(true);
        passwordResetTokenRepository.save(token);
        userProfileRepository.save(user);
        log.info("Password reset completed for {}", user.getEmail());
    }

    private PasswordResetToken getValidToken(String email, String code) {
        UserProfile user = userProfileRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Código de verificación inválido"));

        PasswordResetToken token = passwordResetTokenRepository
                .findTopByUserProfileAndCodeAndUsedFalseOrderByCreatedAtDesc(user, code.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código de verificación inválido"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            token.setUsed(true);
            passwordResetTokenRepository.save(token);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código ha expirado, solicita uno nuevo");
        }

        return token;
    }

    private String generateCode() {
        int code = RANDOM.nextInt(900000) + 100000; // 6 digits, avoids leading zeros
        return Integer.toString(code);
    }

    private void sendCodeEmail(String to, String code) {
        if (!StringUtils.hasText(senderEmail)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No hay remitente configurado para enviar correos");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(to);
        message.setSubject("Código de recuperación de contraseña");
        message.setText("""
                Hola,

                Recibimos una solicitud para restablecer tu contraseña.
                Usa este código para verificar tu identidad:

                %s

                El código vence en %d minutos.

                Si no solicitaste esto, puedes ignorar este mensaje.
                """.formatted(code, expirationMinutes));

        try {
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Error enviando correo de recuperación: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo enviar el correo de verificación");
        }
    }
}
