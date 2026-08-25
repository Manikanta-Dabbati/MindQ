package com.mindq.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

/**
 * TEMPORARY OTP BYPASS CONFIGURATION.
 *
 * Controls whether the OTP bypass feature is active.
 * The bypass activates ONLY when:
 *   app.otp.bypass-enabled=true in application.yml / .env
 *
 * When the flag is absent or false, bypass is OFF.
 * When the flag is true, bypass is ON -- regardless of profile.
 *
 * SAFETY:
 *   - Default is false (bypass OFF)
 *   - Must be explicitly set to true to activate
 *   - This class is intentionally isolated for easy deletion
 *   - REMOVE this and all DevOtp* files when email delivery is fixed
 *
 * USE CASES:
 *   - LOCAL DEVELOPMENT: OTP_BYPASS_ENABLED=true
 *   - RAILWAY TEMPORARY TEST: OTP_BYPASS_ENABLED=true
 *   - NORMAL PRODUCTION: OTP_BYPASS_ENABLED=false (or absent)
 */
@Slf4j
@Configuration
public class DevOtpConfig {

    private final boolean otpBypassEnabled;

    public DevOtpConfig(
            @Value("${app.otp.bypass-enabled:false}") boolean otpBypassEnabled) {
        this.otpBypassEnabled = otpBypassEnabled;

        if (isActive()) {
            log.warn("============================================================");
            log.warn("  [TEMPORARY] OTP BYPASS IS ACTIVE");
            log.warn("  Bypassing OTP verification for REGISTRATION, LOGIN, and");
            log.warn("  PASSWORD_RESET flows.");
            log.warn("  REMOVE OTP_BYPASS_ENABLED when email delivery is fixed.");
            log.warn("============================================================");
        }
    }

    /**
     * Returns true only when the OTP bypass should be active.
     * The flag app.otp.bypass-enabled is the sole control.
     */
    public boolean isActive() {
        return otpBypassEnabled;
    }
}
