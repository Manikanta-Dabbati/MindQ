package com.mindq.auth.service;

import com.mindq.auth.exception.OtpException;
import com.mindq.model.EmailOtp;
import com.mindq.repository.EmailOtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final EmailOtpRepository otpRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_RESENDS = 3;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    @Value("${app.otp.enabled:true}")
    private boolean otpEnabled;

    /**
     * Generate and send an OTP for the given email and purpose.
     */
    @Transactional
    public void generateAndSendOtp(String email, String purpose) {
        if (!otpEnabled) {
            log.warn("OTP disabled — skipping generation for {}", email);
            return;
        }

        // Rate limit: max 3 OTPs per 10 minutes per email+purpose
        long recentCount = otpRepository.countRecentByPurpose(
                email, purpose, LocalDateTime.now().minusMinutes(10));
        if (recentCount >= MAX_RESENDS) {
            throw new OtpException("Too many OTP requests. Please try again later.");
        }

        // Check resend cooldown on existing OTP
        EmailOtp existing = otpRepository
                .findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(email, purpose)
                .orElse(null);

        if (existing != null && existing.getLastResentAt() != null) {
            long secondsSinceLastResend = java.time.Duration.between(
                    existing.getLastResentAt(), LocalDateTime.now()).getSeconds();
            if (secondsSinceLastResend < RESEND_COOLDOWN_SECONDS) {
                long waitSeconds = RESEND_COOLDOWN_SECONDS - secondsSinceLastResend;
                throw new OtpException("Please wait " + waitSeconds + " seconds before requesting a new code.");
            }
        }

        // Invalidate any existing unused OTPs for this email+purpose
        invalidateExistingOtps(email, purpose);

        // Generate 6-digit OTP
        String plainOtp = generateOtpCode();
        String hashedOtp = passwordEncoder.encode(plainOtp);

        // Save hashed OTP
        EmailOtp otp = EmailOtp.builder()
                .email(email)
                .otpCode(hashedOtp)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .attempts(0)
                .used(false)
                .resendCount(recentCount > 0 ? (int) recentCount + 1 : 1)
                .lastResentAt(LocalDateTime.now())
                .build();
        otpRepository.save(otp);

        // Send OTP via email service
        String purposeLabel = switch (purpose) {
            case "REGISTRATION" -> "email verification";
            case "LOGIN" -> "login verification";
            case "PASSWORD_RESET" -> "password reset";
            default -> purpose;
        };
        emailService.sendOtp(email, plainOtp, purposeLabel);

        log.info("OTP generated for {} (purpose={})", email, purpose);
    }

    /**
     * Verify an OTP code. Returns true if valid.
     */
    @Transactional
    public boolean verifyOtp(String email, String otpCode, String purpose) {
        if (!otpEnabled) {
            log.warn("OTP disabled — auto-verifying for {}", email);
            return true;
        }

        EmailOtp otp = otpRepository
                .findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(email, purpose)
                .orElse(null);

        if (otp == null) {
            throw new OtpException("No verification code found. Please request a new one.");
        }

        if (otp.isExpired()) {
            throw new OtpException("That code has expired. Please request a new one.");
        }

        if (otp.isUsed()) {
            throw new OtpException("That code has already been used. Please request a new one.");
        }

        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            throw new OtpException("Too many failed attempts. Please request a new code.");
        }

        // Increment attempts
        otp.setAttempts(otp.getAttempts() + 1);
        otpRepository.save(otp);

        // Verify against hashed OTP
        if (!passwordEncoder.matches(otpCode, otp.getOtpCode())) {
            throw new OtpException("That verification code is incorrect.");
        }

        // Mark as used
        otp.setUsed(true);
        otpRepository.save(otp);

        log.info("OTP verified for {} (purpose={})", email, purpose);
        return true;
    }

    /**
     * Invalidate all existing unused OTPs for a given email and purpose.
     */
    @Transactional
    public void invalidateExistingOtps(String email, String purpose) {
        var existingOtps = otpRepository.findByEmailAndPurposeAndUsedFalse(email, purpose);
        for (EmailOtp otp : existingOtps) {
            otp.setUsed(true);
        }
        otpRepository.saveAll(existingOtps);
    }

    /**
     * Generate a 6-digit OTP code.
     */
    private String generateOtpCode() {
        int code = SECURE_RANDOM.nextInt(900000) + 100000; // 100000–999999
        return String.valueOf(code);
    }

    /**
     * Cleanup expired OTPs (called by scheduler).
     */
    @Transactional
    public void cleanupExpiredOtps() {
        otpRepository.deleteExpiredAndUsed(LocalDateTime.now());
    }
}
