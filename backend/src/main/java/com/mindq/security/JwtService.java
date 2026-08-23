package com.mindq.security;

import com.mindq.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms:86400000}") long accessTokenExpirationMs,
                      @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        // Validate JWT_SECRET at startup - fail fast if missing or weak
        validateJwtSecret(secret);
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    /**
     * Validate JWT_SECRET at application startup.
     * Fails fast if the secret is missing, too short, or matches a known development default.
     */
    private void validateJwtSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET environment variable is not set. " +
                "Set a strong random secret (at least 32 characters) before starting the application.");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException(
                "JWT_SECRET is too short (" + secret.length() + " chars). " +
                "Must be at least 32 characters for HS256 security.");
        }
        // Block known weak/development defaults
        java.util.Set<String> blockedSecrets = java.util.Set.of(
            "dev-only-secret-change-me-in-production-0123456789",
            "production-secret-change-me-in-production-0123456789",
            "change-me-in-production-0123456789",
            "placeholder_secret",
            "your_jwt_secret_here_at_least_32_characters"
        );
        if (blockedSecrets.contains(secret)) {
            throw new IllegalStateException(
                "JWT_SECRET matches a known development default. " +
                "Generate a strong random secret: openssl rand -base64 48");
        }
    }

    /**
     * Generate a short-lived access token for API authorization.
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .claim("type", "access")
                .claim("tokenVersion", user.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpirationMs)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generate a unique refresh token value (opaque, not JWT).
     * The actual refresh token is stored in the database.
     */
    public String generateRefreshTokenValue() {
        return UUID.randomUUID().toString();
    }

    /**
     * @deprecated Use {@link #generateAccessToken(User)} instead.
     */
    @Deprecated
    public String generateToken(User user) {
        return generateAccessToken(user);
    }

    /**
     * Parse and validate an access token.
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }

    public int getTokenVersion(Claims claims) {
        Object v = claims.get("tokenVersion");
        return v != null ? ((Number) v).intValue() : 0;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    @Deprecated
    public long getExpirationSeconds() {
        return getAccessTokenExpirationSeconds();
    }
}
