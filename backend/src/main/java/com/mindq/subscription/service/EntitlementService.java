package com.mindq.subscription.service;

import com.mindq.model.Plan;
import com.mindq.model.User;
import com.mindq.model.UserSubscription;
import com.mindq.repository.PlanRepository;
import com.mindq.repository.UserSubscriptionRepository;
import com.mindq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntitlementService {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final UserSubscriptionRepository subscriptionRepository;

    @Value("${mindq.storage.free-limit-bytes:524288000}") // 500 MB default
    private long defaultFreeStorageBytes;

    /**
     * Get the user's current plan, defaulting to FREE if no subscription exists.
     */
    @Transactional(readOnly = true)
    public Plan getUserPlan(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Optional<UserSubscription> subOpt = subscriptionRepository.findByUserIdAndStatusWithPlan(user.getId(), "ACTIVE");

        if (subOpt.isPresent()) {
            UserSubscription sub = subOpt.get();
            // Check if subscription has expired
            if (sub.getEndDate() != null && LocalDateTime.now().isAfter(sub.getEndDate())) {
                return getFreePlan();
            }
            return sub.getPlan();
        }

        return getFreePlan();
    }

    /**
     * Check if user can upload a file of given size.
     */
    @Transactional(readOnly = true)
    public boolean canUpload(String email, long fileSizeBytes, long currentUsedBytes) {
        Plan plan = getUserPlan(email);
        return currentUsedBytes + fileSizeBytes <= plan.getStorageLimitBytes();
    }

    /**
     * Get storage limit in bytes for user's plan.
     */
    @Transactional(readOnly = true)
    public long getStorageLimit(String email) {
        return getUserPlan(email).getStorageLimitBytes();
    }

    /**
     * Check if user can generate AI content (within daily limit).
     */
    @Transactional(readOnly = true)
    public boolean canGenerateAi(String email, int generationsToday) {
        Plan plan = getUserPlan(email);
        return generationsToday < plan.getDailyAiGenerations();
    }

    /**
     * Get max questions per generation for user's plan.
     */
    @Transactional(readOnly = true)
    public int getMaxQuestionsPerGeneration(String email) {
        return getUserPlan(email).getMaxQuestionsPerGeneration();
    }

    /**
     * Check if user has access to advanced AI models.
     */
    @Transactional(readOnly = true)
    public boolean hasAdvancedModels(String email) {
        return getUserPlan(email).getAdvancedModels();
    }

    /**
     * Check if user has AI tutor access.
     */
    @Transactional(readOnly = true)
    public boolean hasAiTutor(String email) {
        return getUserPlan(email).getAiTutor();
    }

    /**
     * Check if user can export to PDF/DOCX.
     */
    @Transactional(readOnly = true)
    public boolean hasExportFormats(String email) {
        return getUserPlan(email).getExportFormats();
    }

    /**
     * Get remaining storage in bytes.
     */
    @Transactional(readOnly = true)
    public long getRemainingStorage(String email, long currentUsedBytes) {
        long limit = getStorageLimit(email);
        return Math.max(0, limit - currentUsedBytes);
    }

    /**
     * Get storage usage percentage (0-100).
     */
    @Transactional(readOnly = true)
    public double getStoragePercentage(String email, long currentUsedBytes) {
        long limit = getStorageLimit(email);
        if (limit == 0) return 100.0;
        return Math.min(100.0, (currentUsedBytes * 100.0) / limit);
    }

    /**
     * Assign the FREE plan to a new user during registration.
     */
    @Transactional
    public void assignFreePlan(User user) {
        Plan freePlan = getFreePlan();
        UserSubscription sub = UserSubscription.builder()
                .user(user)
                .plan(freePlan)
                .status("ACTIVE")
                .startDate(LocalDateTime.now())
                .build();
        subscriptionRepository.save(sub);
        log.info("Assigned FREE plan to user {}", user.getEmail());
    }

    private Plan getFreePlan() {
        return planRepository.findByCode("FREE")
                .orElseGet(() -> {
                    // Fallback: create FREE plan if not seeded
                    Plan free = Plan.builder()
                            .code("FREE")
                            .displayName("Free")
                            .storageLimitBytes(defaultFreeStorageBytes)
                            .dailyAiGenerations(20)
                            .maxQuestionsPerGeneration(20)
                            .advancedModels(false)
                            .aiTutor(false)
                            .exportFormats(false)
                            .prioritySupport(false)
                            .build();
                    return planRepository.save(free);
                });
    }
}
