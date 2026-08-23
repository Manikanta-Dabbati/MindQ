package com.mindq.subscription.controller;

import com.mindq.common.api.ApiResponse;
import com.mindq.model.Plan;
import com.mindq.model.User;
import com.mindq.model.UserSubscription;
import com.mindq.repository.PlanRepository;
import com.mindq.repository.UserRepository;
import com.mindq.repository.UserSubscriptionRepository;
import com.mindq.subscription.dto.PlanResponse;
import com.mindq.subscription.dto.SubscriptionResponse;
import com.mindq.subscription.service.EntitlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class PlanController {

    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final EntitlementService entitlementService;

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<PlanResponse>>> getAllPlans() {
        List<PlanResponse> plans = planRepository.findAll().stream()
                .map(this::toPlanResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(plans, "Plans retrieved"));
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getCurrentSubscription(
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        Plan plan = entitlementService.getUserPlan(user.getEmail());

        SubscriptionResponse subResponse = SubscriptionResponse.builder()
                .plan(toPlanResponse(plan))
                .status("ACTIVE")
                .build();

        // Try to find actual subscription details
        subscriptionRepository.findByUserIdAndStatusWithPlan(user.getId(), "ACTIVE")
                .ifPresent(sub -> {
                    subResponse.setSubscriptionId(sub.getId());
                    subResponse.setStartDate(sub.getStartDate());
                    subResponse.setEndDate(sub.getEndDate());
                    subResponse.setCancelledAt(sub.getCancelledAt());
                    subResponse.setStatus(sub.getStatus());
                });

        return ResponseEntity.ok(ApiResponse.success(subResponse, "Subscription retrieved"));
    }

    private PlanResponse toPlanResponse(Plan plan) {
        return PlanResponse.builder()
                .id(plan.getId())
                .code(plan.getCode())
                .displayName(plan.getDisplayName())
                .description(plan.getDescription())
                .storageLimitBytes(plan.getStorageLimitBytes())
                .dailyAiGenerations(plan.getDailyAiGenerations())
                .maxQuestionsPerGeneration(plan.getMaxQuestionsPerGeneration())
                .advancedModels(plan.getAdvancedModels())
                .aiTutor(plan.getAiTutor())
                .exportFormats(plan.getExportFormats())
                .prioritySupport(plan.getPrioritySupport())
                .priceInPaise(plan.getPriceInPaise())
                .build();
    }
}
