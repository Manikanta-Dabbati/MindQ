package com.mindq.analytics.controller;

import com.mindq.analytics.dto.AnalyticsOverview;
import com.mindq.analytics.dto.TopicPerformance;
import com.mindq.analytics.service.AnalyticsService;
import com.mindq.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AnalyticsOverview>> getOverview(Authentication authentication) {
        AnalyticsOverview overview = analyticsService.getOverview(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(overview, "Analytics retrieved successfully"));
    }

    @GetMapping("/topics")
    public ResponseEntity<ApiResponse<List<TopicPerformance>>> getTopicPerformance(Authentication authentication) {
        List<TopicPerformance> topics = analyticsService.getTopicPerformance(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(topics, "Topic performance retrieved successfully"));
    }
}
