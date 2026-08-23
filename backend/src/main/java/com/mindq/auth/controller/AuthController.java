package com.mindq.auth.controller;

import com.mindq.auth.dto.*;
import com.mindq.auth.service.AuthService;
import com.mindq.auth.service.UserService;
import com.mindq.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        AuthResponse auth = authService.login(request, ipAddress);
        return ResponseEntity.ok(ApiResponse.success(auth, "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse auth = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(auth, "Token refreshed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        authService.logout(request != null ? request.getRefreshToken() : null);
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(Authentication authentication) {
        authService.logoutAll(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out from all devices"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(Authentication authentication) {
        UserResponse user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(user, "Current user"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "If the email exists, a reset link has been sent"));
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<ApiResponse<VerifyResetOtpResponse>> verifyResetOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        String resetToken = authService.verifyResetOtp(request.getEmail(), request.getOtpCode());
        return ResponseEntity.ok(ApiResponse.success(
                new VerifyResetOtpResponse(resetToken), "OTP verified. You can now set a new password."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successful"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse user = authService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(user, "Profile updated successfully"));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(Authentication authentication) {
        authService.deleteAccount(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Account deleted successfully"));
    }

    // ── Email OTP endpoints ──────────────────────────────────────

    @PostMapping("/verify-email-otp")
    public ResponseEntity<ApiResponse<Void>> verifyEmailOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyEmailOtp(request.getEmail(), request.getOtpCode());
        return ResponseEntity.ok(ApiResponse.success(null, "Email verified successfully"));
    }

    @PostMapping("/request-login-otp")
    public ResponseEntity<ApiResponse<Void>> requestLoginOtp(
            @Valid @RequestBody RequestOtpRequest request) {
        authService.requestLoginOtp(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "Verification code sent"));
    }

    @PostMapping("/verify-login-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyLoginOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        AuthResponse auth = authService.verifyLoginOtp(request.getEmail(), request.getOtpCode(), ipAddress);
        return ResponseEntity.ok(ApiResponse.success(auth, "Login successful"));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(
            @Valid @RequestBody RequestOtpRequest request) {
        authService.resendOtp(request.getEmail(), request.getPurpose());
        return ResponseEntity.ok(ApiResponse.success(null, "Verification code resent"));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
