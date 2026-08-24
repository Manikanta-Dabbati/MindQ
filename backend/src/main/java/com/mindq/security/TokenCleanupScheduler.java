package com.mindq.security;

import com.mindq.auth.service.OtpService;
import com.mindq.auth.service.TokenService;
import com.mindq.repository.EmailLogRepository;
import com.mindq.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Scheduled task to clean up expired tokens.
 * Runs every hour to prevent unbounded database growth.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final TokenService tokenService;
    private final OtpService otpService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailLogRepository emailLogRepository;

    /**
     * Clean up expired refresh tokens every hour.
     */
    @Scheduled(fixedRate = 3600000, initialDelay = 3600000)
    public void cleanupExpiredRefreshTokens() {
        try {
            tokenService.cleanupExpiredTokens();
            log.debug("Cleaned up expired refresh tokens");
        } catch (Exception e) {
            log.error("Failed to clean up expired refresh tokens", e);
        }
    }

    /**
     * Clean up expired/used password reset tokens every hour.
     */
    @Scheduled(fixedRate = 3600000, initialDelay = 1800000)
    public void cleanupExpiredPasswordResetTokens() {
        try {
            passwordResetTokenRepository.deleteExpiredAndUsed(LocalDateTime.now());
            log.debug("Cleaned up expired password reset tokens");
        } catch (Exception e) {
            log.error("Failed to clean up expired password reset tokens", e);
        }
    }

    @Scheduled(fixedRate = 3600000, initialDelay = 900000)
    public void cleanupExpiredOtps() {
        try {
            otpService.cleanupExpiredOtps();
            log.debug("Cleaned up expired OTPs");
        } catch (Exception e) {
            log.error("Failed to clean up expired OTPs", e);
        }
    }

    /**
     * Clean up email logs older than 30 days every 6 hours.
     */
    @Scheduled(fixedRate = 21600000, initialDelay = 21600000)
    public void cleanupOldEmailLogs() {
        try {
            int deleted = emailLogRepository.deleteOlderThan(LocalDateTime.now().minusDays(30));
            if (deleted > 0) {
                log.info("Cleaned up {} old email logs", deleted);
            }
        } catch (Exception e) {
            log.error("Failed to clean up old email logs", e);
        }
    }
}
