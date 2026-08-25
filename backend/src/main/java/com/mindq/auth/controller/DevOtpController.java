package com.mindq.auth.controller;

import com.mindq.auth.config.DevOtpConfig;
import com.mindq.auth.dto.AuthResponse;
import com.mindq.auth.exception.InvalidCredentialsException;
import com.mindq.enums.UserStatus;
import com.mindq.model.PasswordResetToken;
import com.mindq.model.User;
import com.mindq.repository.PasswordResetTokenRepository;
import com.mindq.repository.EmailOtpRepository;
import com.mindq.repository.UserRepository;
import com.mindq.security.JwtService;
import com.mindq.subscription.service.EntitlementService;
import com.mindq.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DEVELOPMENT ONLY -- OTP bypass controller.
 *
 * Provides endpoints to bypass OTP verification during development.
 * These endpoints are only active when DevOtpConfig.isActive() returns true.
 * When bypass is inactive, these endpoints return 404.
 *
 * THIS FILE MUST BE DELETED BEFORE PRODUCTION DEPLOYMENT.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/dev")
@RequiredArgsConstructor
public class DevOtpController {

    private final DevOtpConfig devOtpConfig;
    private final EmailOtpRepository emailOtpRepository;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final EntitlementService entitlementService;

    @Value("${app.auth.password-reset-expiration-minutes:30}")
    private int passwordResetExpirationMinutes;

    /**
     * Returns whether the OTP bypass is currently active.
     * Used by the frontend to decide whether to auto-verify.
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(Map.of(
                "bypassEnabled", devOtpConfig.isActive(),
                "warning", "Development only -- not for production use"
        ));
    }

    /**
     * Auto-verify OTP for the given email and purpose.
     * Only active when DevOtpConfig.isActive() returns true.
     *
     * Supported purposes:
     *   REGISTRATION  -- activates the user account
     *   LOGIN         -- returns auth tokens (JWT + refresh)
     *   PASSWORD_RESET -- returns a short-lived reset token
     */
    @PostMapping("/auto-verify")
    public ResponseEntity<?> autoVerify(@RequestBody Map<String, String> request) {
        if (!devOtpConfig.isActive()) {
            return ResponseEntity.notFound().build();
        }

        String email = request.get("email");
        String purpose = request.get("purpose");

        if (email == null || email.isBlank() || purpose == null || purpose.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "email and purpose are required"
            ));
        }

        String normalizedEmail = email.trim().toLowerCase();

        // SECURITY: Verify the user initiated the flow by checking for a pending OTP.
        // This prevents arbitrary account takeover — the user must have:
        //   REGISTRATION: called POST /register (which generates an OTP)
        //   LOGIN: called POST /request-login-otp (which generates an OTP)
        //   PASSWORD_RESET: called POST /forgot-password (which generates an OTP)
        boolean hasPendingOtp = emailOtpRepository
                .findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(normalizedEmail, purpose)
                .isPresent();

        if (!hasPendingOtp) {
            log.warn("[DEV OTP BYPASS] BLOCKED: No pending OTP for {} (purpose={})",
                    normalizedEmail, purpose);
            return ResponseEntity.notFound().build();
        }

        log.warn("[DEV OTP BYPASS] Auto-verifying OTP for {} (purpose={})", normalizedEmail, purpose);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        return switch (purpose) {
            case "REGISTRATION" -> handleRegistrationBypass(user);
            case "LOGIN" -> handleLoginBypass(user);
            case "PASSWORD_RESET" -> handlePasswordResetBypass(user);
            default -> ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Unsupported purpose: " + purpose
            ));
        };
    }

    private ResponseEntity<?> handleRegistrationBypass(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            log.warn("[DEV OTP BYPASS] Activated user {} (was UNVERIFIED)", user.getEmail());
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Email verified (dev bypass)"
        ));
    }

    private ResponseEntity<?> handleLoginBypass(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException("Account is not active");
        }

        // Issue real JWT tokens -- same as verifyLoginOtp in AuthService
        user.resetFailedLoginAttempts();
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = tokenService.createRefreshToken(user);

        log.warn("[DEV OTP BYPASS] Issued tokens for login: {}", user.getEmail());

        AuthResponse authResponse = AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInSeconds(jwtService.getAccessTokenExpirationSeconds())
                .refreshExpiresInSeconds(jwtService.getRefreshTokenExpirationMs() / 1000)
                .user(com.mindq.auth.dto.UserResponse.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .createdAt(user.getCreatedAt())
                        .build())
                .build();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Login successful (dev bypass)",
                "data", authResponse
        ));
    }

    private ResponseEntity<?> handlePasswordResetBypass(User user) {
        // Generate a short-lived reset token -- same as verifyResetOtp in AuthService
        String tokenValue = jwtService.generateRefreshTokenValue();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(tokenValue)
                .expiresAt(LocalDateTime.now().plusMinutes(passwordResetExpirationMinutes))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);

        log.warn("[DEV OTP BYPASS] Issued reset token for {}", user.getEmail());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OTP verified (dev bypass)",
                "data", Map.of("resetToken", tokenValue)
        ));
    }
}
