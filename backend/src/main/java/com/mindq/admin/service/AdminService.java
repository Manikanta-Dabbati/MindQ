package com.mindq.admin.service;

import com.mindq.admin.dto.AdminDashboardStats;
import com.mindq.admin.dto.AdminUserResponse;
import com.mindq.auth.service.AuthService;
import com.mindq.model.User;
import com.mindq.model.UserSubscription;
import com.mindq.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final StudyMaterialRepository materialRepository;
    private final McqSetRepository mcqSetRepository;
    private final GenerationHistoryRepository generationHistoryRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public AdminDashboardStats getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalMaterials = materialRepository.count();
        long totalQuizzes = mcqSetRepository.count();
        long totalGenerations = generationHistoryRepository.count();
        long activeUsers = totalUsers;
        long totalRevenue = paymentTransactionRepository.sumSuccessfulAmount();
        long storageUsed = materialRepository.sumAllFileSizeBytes();

        return AdminDashboardStats.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalMaterials(totalMaterials)
                .totalQuizzes(totalQuizzes)
                .totalAiGenerations(totalGenerations)
                .totalRevenue(totalRevenue)
                .storageUsedBytes(storageUsed)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();

        Map<Long, UserSubscription> subscriptionByUserId = subscriptionRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toMap(
                        us -> us.getUser().getId(),
                        us -> us,
                        (a, b) -> a
                ));

        Map<Long, Long> materialCountByUserId = new HashMap<>();
        materialRepository.findAll().forEach(m ->
            materialCountByUserId.merge(m.getUser().getId(), 1L, Long::sum));

        Map<Long, Long> quizCountByUserId = new HashMap<>();
        mcqSetRepository.findAll().forEach(q ->
            quizCountByUserId.merge(q.getUser().getId(), 1L, Long::sum));

        return users.stream()
                .map(user -> {
                    var sub = subscriptionByUserId.get(user.getId());
                    return AdminUserResponse.builder()
                            .id(user.getId())
                            .email(user.getEmail())
                            .fullName(user.getFullName())
                            .role(user.getRole().name())
                            .status(user.getStatus().name())
                            .planCode(sub != null ? sub.getPlan().getCode() : "FREE")
                            .materialCount(materialCountByUserId.getOrDefault(user.getId(), 0L))
                            .quizCount(quizCountByUserId.getOrDefault(user.getId(), 0L))
                            .createdAt(user.getCreatedAt())
                            .build();
                })
                .toList();
    }

    @Transactional
    public void updateUserStatus(Long userId, com.mindq.enums.UserStatus status, Long adminId) {
        if (userId.equals(adminId) && (status == com.mindq.enums.UserStatus.INACTIVE || status == com.mindq.enums.UserStatus.BANNED)) {
            throw new IllegalArgumentException("You cannot deactivate or ban your own administrator account.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setStatus(status);
        userRepository.save(user);
        log.info("Admin {} updated user {} status to {}", adminId, userId, status);
    }

    @Transactional
    public void deleteUser(Long userId, Long adminId) {
        if (userId.equals(adminId)) {
            throw new IllegalArgumentException("You cannot delete your own administrator account.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        log.info("Admin {} deleting user {} ({})", adminId, userId, user.getEmail());
        // Delegate to AuthService which handles complete data cleanup
        authService.deleteAccount(user.getEmail());
        log.info("User {} fully deleted by admin {}", userId, adminId);
    }

    @Transactional
    public void updateUserRole(Long userId, com.mindq.enums.UserRole role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRole(role);
        userRepository.save(user);
        log.info("Admin updated user {} role to {}", userId, role);
    }
}
