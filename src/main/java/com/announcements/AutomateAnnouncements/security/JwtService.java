package com.announcements.AutomateAnnouncements.security;

import com.announcements.AutomateAnnouncements.entities.UserProfile;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    private static final int MIN_KEY_LENGTH_BYTES = 32;

    private final Key signingKey;
    private final long jwtExpirationMs;

    public JwtService(
            @Value("${application.security.jwt.secret:change-me-please-very-long-secret-key}") String secretKey,
            @Value("${application.security.jwt.expiration:3600000}") long jwtExpirationMs) {
        this.signingKey = buildSigningKey(secretKey);
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserProfile userProfile) {
        Map<String, Object> claims = Map.of(
                "email", userProfile.getEmail(),
                "displayName", userProfile.getDisplayName(),
                "authUserId", userProfile.getAuthUserId(),
                "role", userProfile.getRole()
        );

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userProfile.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserProfileDetails userDetails) {
        final String username = extractUsername(token);
        return username.equalsIgnoreCase(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key buildSigningKey(String providedSecret) {
        if (!StringUtils.hasText(providedSecret)) {
            throw new IllegalStateException("JWT secret is not configured. Set APPLICATION_SECURITY_JWT_SECRET to a random string with at least 32 characters.");
        }

        byte[] keyBytes = providedSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_KEY_LENGTH_BYTES) {
            log.warn("JWT secret is only {} bytes. Hashing it to derive a 256-bit signing key. Please update APPLICATION_SECURITY_JWT_SECRET with a longer random value.",
                    keyBytes.length);
            keyBytes = hashSecret(providedSecret);
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] hashSecret(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in the current JVM", e);
        }
    }
}
