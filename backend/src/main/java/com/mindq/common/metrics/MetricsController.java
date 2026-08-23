package com.mindq.common.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;

    @GetMapping("/metrics")
    public ResponseEntity<MetricsService.MetricsSnapshot> metrics() {
        return ResponseEntity.ok(metricsService.getSnapshot());
    }
}
