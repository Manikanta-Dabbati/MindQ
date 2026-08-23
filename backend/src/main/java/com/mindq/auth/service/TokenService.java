package com.mindq.auth.service;

import com.mindq.auth.exception.InvalidRefreshTokenException;
import com.mindq.model.RefreshToken;
import com.mindq.model.User;
import com.mindq.repository.RefreshTokenRepository;
import com.mindq.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    private static final int MAX_ACTIVE_REFRESH_TOKENS = 5;

    /**
     * Create a new refresh token for the user.
     * Enforces a maximum number of active refresh tokens (device limit).
     */
    @Transactional
    public String createRefreshToken(User user) {
        // Enforce device limit: revoke oldest tokens if too many active
        var activeTokens = refreshTokenRepository.findByUserAndRevokedFalse(user);
        if (activeTokens.size() >= MAX_ACTIVE_REFRESH_TOKENS) {
            // Revoke the oldest token
            var oldest = activeTokens.stream()
                    .min((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                    .orElse(null);
            if (oldest != null) {
                oldest.setRevoked(true);
                refreshTokenRepository.save(oldest);
                log.info("Revoked oldest refresh token for user {} (device limit)", user.getEmail());
            }
        }

        String tokenValue = jwtService.generateRefreshTokenValue();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenValue)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtService.getRefreshTokenExpirationMs() / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        return tokenValue;
    }

    /**
     * Result of a successful refresh token rotation.
     */
    public record RefreshResult(User user, String newRefreshToken) {}

    /**
     * Validate and rotate a refresh token.
     * Returns a RefreshResult with the user and new refresh token value.
     * This avoids the bug where findRefreshToken is called after revocation.
     */
    @Transactional
    public RefreshResult rotateRefreshToken(String tokenValue) {
        RefreshToken oldToken = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (!oldToken.isValid()) {
            if (oldToken.isRevoked()) {
                // Possible token reuse attack — revoke all tokens for this user
                log.warn("Refresh token reuse detected for user {}. Revoking all tokens.", 
                        oldToken.getUser().getEmail());
                refreshTokenRepository.revokeAllByUser(oldToken.getUser());
            }
            throw new InvalidRefreshTokenException("Refresh token is expired or revoked");
        }

        User user = oldToken.getUser();

        // Revoke the old token (rotation)
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        // Issue a new one
        String newRefreshToken = createRefreshToken(user);
        return new RefreshResult(user, newRefreshToken);
    }

    /**
     * Revoke a single refresh token (logout).
     */
    @Transactional
    public void revokeToken(String tokenValue) {
        refreshTokenRepository.findByToken(tokenValue)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    /**
     * Revoke all refresh tokens for a user (logout everywhere).
     */
    @Transactional
    public void revokeAllTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    /**
     * Find a refresh token entity by value.
     */
    @Transactional(readOnly = true)
    public RefreshToken findRefreshToken(String tokenValue) {
        return refreshTokenRepository.findByToken(tokenValue).orElse(null);
    }

    /**
     * Cleanup expired tokens (schedule periodically).
     */
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredAndRevoked(LocalDateTime.now());
    }
}
