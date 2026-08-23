package com.mindq.auth.service;

import com.mindq.auth.dto.*;
import com.mindq.auth.exception.*;
import com.mindq.enums.UserStatus;
import com.mindq.model.FailedLoginAttempt;
import com.mindq.model.PasswordResetToken;
import com.mindq.model.User;
import com.mindq.repository.FailedLoginAttemptRepository;
import com.mindq.repository.PasswordResetTokenRepository;
import com.mindq.repository.PaymentTransactionRepository;
import com.mindq.repository.*;
import com.mindq.common.metrics.MetricsService;
import com.mindq.security.JwtService;
import com.mindq.subscription.service.EntitlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final FailedLoginAttemptRepository failedLoginAttemptRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordValidator passwordValidator;
    private final EntitlementService entitlementService;
    private final OtpService otpService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final SavedQuestionRepository savedQuestionRepository;
    private final GenerationHistoryRepository generationHistoryRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final FlashcardRepository flashcardRepository;
    private final FlashcardSetRepository flashcardSetRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final McqSetRepository mcqSetRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final StudyMaterialRepository materialRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final MetricsService metricsService;

    @Value("${app.auth.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.auth.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes;

    @Value("${app.auth.password-reset-expiration-minutes:30}")
    private int passwordResetExpirationMinutes;

    /**
     * Login with email + password.
     * Returns access token + refresh token.
     * Enforces account lockout after repeated failures.
     */
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    recordFailedLogin(email, ipAddress);
                    return new InvalidCredentialsException("Invalid email or password");
                });

        if (user.getStatus() == UserStatus.UNVERIFIED) {
            throw new AccountNotActiveException("Please verify your email before logging in. Check your inbox for the verification code.");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new AccountNotActiveException("Your account has been deactivated. Please contact support.");
        }
        if (user.getStatus() == UserStatus.BANNED) {
            throw new AccountNotActiveException("Your account has been suspended. Please contact support.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountNotActiveException("Account is not active");
        }

        if (user.isLocked()) {
            long remainingSeconds = java.time.Duration.between(
                    LocalDateTime.now(), user.getLockedUntil()).getSeconds();
            throw new AccountLockedException(
                    "Account is locked due to too many failed attempts. Try again in " +
                            (remainingSeconds / 60 + 1) + " minutes.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordFailedLogin(email, ipAddress);
            user.incrementFailedLoginAttempts();

            if (user.getFailedLoginAttempts() >= maxFailedAttempts) {
                user.lockAccount(lockoutDurationMinutes);
                log.warn("Account locked for {} after {} failed attempts", email, maxFailedAttempts);
            }
            userRepository.save(user);
            metricsService.recordLoginAttempt(false);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Successful login — reset failed attempts and issue tokens
        user.resetFailedLoginAttempts();
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = tokenService.createRefreshToken(user);

        metricsService.recordLoginAttempt(true);
        log.info("Successful login for user {}", email);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInSeconds(jwtService.getAccessTokenExpirationSeconds())
                .refreshExpiresInSeconds(jwtService.getRefreshTokenExpirationMs() / 1000)
                .user(toUserResponse(user))
                .build();
    }

    /**
     * Refresh an access token using a valid refresh token (rotation).
     */
    @Transactional
    public AuthResponse refreshAccessToken(String refreshTokenValue) {
        // rotateRefreshToken validates, revokes old, creates new, and returns user + new token
        TokenService.RefreshResult result = tokenService.rotateRefreshToken(refreshTokenValue);
        
        String newAccessToken = jwtService.generateAccessToken(result.user());

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(result.newRefreshToken())
                .tokenType("Bearer")
                .expiresInSeconds(jwtService.getAccessTokenExpirationSeconds())
                .refreshExpiresInSeconds(jwtService.getRefreshTokenExpirationMs() / 1000)
                .user(toUserResponse(result.user()))
                .build();
    }

    /**
     * Logout — revoke the refresh token.
     */
    @Transactional
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue != null) {
            tokenService.revokeToken(refreshTokenValue);
        }
    }

    /**
     * Logout from all devices — revoke all refresh tokens.
     */
    @Transactional
    public void logoutAll(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
        tokenService.revokeAllTokens(user);
        user.incrementTokenVersion();
        userRepository.save(user);
        log.info("All sessions revoked for user {} (tokenVersion={})", email, user.getTokenVersion());
    }

    /**
     * Request a password reset via OTP. Sends a 6-digit code to the user's email.
     * Always returns success to prevent email enumeration.
     */
    @Transactional
    public void forgotPassword(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user != null && user.getStatus() == UserStatus.ACTIVE) {
            otpService.generateAndSendOtp(normalizedEmail, "PASSWORD_RESET");
            log.info("Password reset OTP sent for {}", normalizedEmail);
        }

        // Always return success to prevent email enumeration
    }

    /**
     * Verify the password-reset OTP and return a short-lived reset token.
     */
    @Transactional
    public String verifyResetOtp(String email, String otpCode) {
        String normalizedEmail = email.trim().toLowerCase();

        otpService.verifyOtp(normalizedEmail, otpCode, "PASSWORD_RESET");

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        // Generate a short-lived reset token so the existing resetPassword() flow works
        String tokenValue = jwtService.generateRefreshTokenValue();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(tokenValue)
                .expiresAt(LocalDateTime.now().plusMinutes(passwordResetExpirationMinutes))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset OTP verified for {} — reset token issued", normalizedEmail);
        return tokenValue;
    }

    /**
     * Reset password using a valid reset token.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new PasswordResetException("Invalid reset token"));

        if (!resetToken.isValid()) {
            throw new PasswordResetException("Reset token is expired or already used");
        }

        // Validate new password strength
        passwordValidator.validate(request.getNewPassword());

        // Update password
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.resetFailedLoginAttempts(); // Reset lockout on password change
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Revoke all refresh tokens (force re-login on all devices)
        tokenService.revokeAllTokens(user);

        metricsService.recordPasswordReset();
        log.info("Password reset completed for {}", user.getEmail());
    }

    /**
     * Change password (authenticated user).
     */
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        passwordValidator.validate(request.getNewPassword());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed for {}", email);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        return toUserResponse(user);
    }

    /**
     * Update user profile (name only).
     */
    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        user.setFullName(request.getFullName().trim());
        userRepository.save(user);

        log.info("Profile updated for {}", email);
        return toUserResponse(user);
    }

    /**
     * Delete user account and all associated data.
     * Revokes all tokens first.
     */
    @Transactional
    public void deleteAccount(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
        Long userId = user.getId();

        // 1. Revoke and delete all refresh tokens
        tokenService.revokeAllTokens(user);
        refreshTokenRepository.deleteAllByUser(user);

        // 2. Delete password reset tokens for this user
        passwordResetTokenRepository.deleteByUserId(userId);

        // 3. Delete email OTPs for this user
        emailOtpRepository.deleteByEmail(email);

        // 4. Delete failed login attempts
        failedLoginAttemptRepository.deleteByEmail(email);

        // 5. Delete saved questions
        savedQuestionRepository.deleteByUserId(userId);

        // 6. Delete generation history
        generationHistoryRepository.deleteByUserId(userId);

        // 7. Delete quiz answers and attempts (quiz_attempts -> quiz_answers)
        quizAnswerRepository.deleteByQuizAttemptUserId(userId);
        quizAttemptRepository.deleteByUserId(userId);

        // 8. Delete flashcards via flashcard sets
        flashcardRepository.deleteByFlashcardSetUserId(userId);
        flashcardSetRepository.deleteByUserId(userId);

        // 9. Delete payment transactions
        paymentTransactionRepository.deleteByUserId(userId);

        // 10. Delete user subscriptions
        userSubscriptionRepository.deleteByUserId(userId);

        // 11. Delete question options (must be before questions due to FK)
        questionOptionRepository.deleteByMcqSetUserId(userId);

        // 12. Delete questions (must be before mcq_sets due to FK)
        questionRepository.deleteByMcqSetUserId(userId);

        // 13. Delete MCQ sets
        mcqSetRepository.deleteByUserId(userId);

        // 14. Delete study materials
        materialRepository.deleteByUserId(userId);

        // 15. Delete the user
        userRepository.delete(user);

        metricsService.recordAccountDeletion();
        log.info("Account fully deleted for {} (userId={})", email, userId);
    }

    // ── Email OTP methods ────────────────────────────────────────

    /**
     * Verify email OTP for registration. Activates the user account.
     */
    @Transactional
    public void verifyEmailOtp(String email, String otpCode) {
        String normalizedEmail = email.trim().toLowerCase();

        otpService.verifyOtp(normalizedEmail, otpCode, "REGISTRATION");

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (user.getStatus() == UserStatus.ACTIVE) {
            return; // Already verified
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        metricsService.recordRegistration();
        log.info("Email verified for user {}", normalizedEmail);
    }

    /**
     * Request an OTP for email-based login.
     */
    @Transactional
    public void requestLoginOtp(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElse(null);

        // Always return success to prevent email enumeration
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            log.info("Login OTP requested for non-existent or inactive email");
            return;
        }

        otpService.generateAndSendOtp(normalizedEmail, "LOGIN");
    }

    /**
     * Verify OTP for email-based login. Returns auth tokens.
     */
    @Transactional
    public AuthResponse verifyLoginOtp(String email, String otpCode, String ipAddress) {
        String normalizedEmail = email.trim().toLowerCase();

        otpService.verifyOtp(normalizedEmail, otpCode, "LOGIN");

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountNotActiveException("Account is not active");
        }

        if (user.isLocked()) {
            throw new AccountLockedException("Account is locked");
        }

        // Successful login — reset failed attempts and issue tokens
        user.resetFailedLoginAttempts();
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = tokenService.createRefreshToken(user);

        log.info("OTP login successful for user {}", normalizedEmail);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInSeconds(jwtService.getAccessTokenExpirationSeconds())
                .refreshExpiresInSeconds(jwtService.getRefreshTokenExpirationMs() / 1000)
                .user(toUserResponse(user))
                .build();
    }

    /**
     * Resend OTP for any purpose.
     */
    @Transactional
    public void resendOtp(String email, String purpose) {
        String normalizedEmail = email.trim().toLowerCase();
        otpService.generateAndSendOtp(normalizedEmail, purpose);
    }

    private void recordFailedLogin(String email, String ipAddress) {
        FailedLoginAttempt attempt = FailedLoginAttempt.builder()
                .email(email)
                .ipAddress(ipAddress != null ? ipAddress : "unknown")
                .build();
        failedLoginAttemptRepository.save(attempt);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
