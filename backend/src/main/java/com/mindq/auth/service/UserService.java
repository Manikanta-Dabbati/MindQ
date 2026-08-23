package com.mindq.auth.service;

import com.mindq.auth.dto.RegisterRequest;
import com.mindq.auth.dto.UserResponse;
import com.mindq.auth.exception.EmailAlreadyExistsException;
import com.mindq.enums.UserStatus;
import com.mindq.model.User;
import com.mindq.repository.UserRepository;
import com.mindq.subscription.service.EntitlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntitlementService entitlementService;
    private final OtpService otpService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Normalize so "Manideep@Example.COM" and "manideep@example.com" are the same account.
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.UNVERIFIED)
                .build();

        User saved = userRepository.save(user);

        // Assign FREE plan to new user
        entitlementService.assignFreePlan(saved);

        // Generate and send verification OTP
        try {
            otpService.generateAndSendOtp(email, "REGISTRATION");
        } catch (Exception e) {
            log.error("Failed to send verification OTP to {}: {}", email, e.getMessage());
            // Don't fail registration if OTP sending fails — user can resend
        }

        log.info("User registered (UNVERIFIED): {}", email);

        return UserResponse.builder()
                .id(saved.getId())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .role(saved.getRole())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
