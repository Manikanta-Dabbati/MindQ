package com.mindq.common.controller;

import com.mindq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final UserRepository userRepository;

    @Value("${app.ai.groq.api-key:}")
    private String groqApiKey;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("service", "mindq-backend");
        result.put("timestamp", Instant.now().toString());

        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("database", checkDatabase());
        checks.put("ai", checkAiProvider());
        checks.put("users", Map.of("status", "UP", "count", userRepository.count()));
        result.put("checks", checks);

        boolean allUp = checks.values().stream()
                .allMatch(c -> c instanceof Map m && "UP".equals(m.get("status")));
        if (!allUp) {
            result.put("status", "DEGRADED");
            return ResponseEntity.status(503).body(result);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health/live")
    public ResponseEntity<Map<String, String>> live() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "mindq-backend", "timestamp", Instant.now().toString()));
    }

    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        Map<String, String> dbCheck = checkDatabase();
        boolean ready = "UP".equals(dbCheck.get("status"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", ready ? "UP" : "DOWN");
        result.put("database", dbCheck);
        result.put("timestamp", Instant.now().toString());
        return ready ? ResponseEntity.ok(result) : ResponseEntity.status(503).body(result);
    }

    private Map<String, String> checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(3);
            return Map.of("status", valid ? "UP" : "DOWN", "message", valid ? "Connected" : "Connection invalid");
        } catch (Exception e) {
            return Map.of("status", "DOWN", "message", e.getMessage() != null ? e.getMessage() : "Connection failed");
        }
    }

    private Map<String, String> checkAiProvider() {
        boolean configured = groqApiKey != null && !groqApiKey.isBlank();
        return Map.of("status", configured ? "UP" : "DEGRADED", "provider", "groq", "message", configured ? "API key configured" : "No API key configured");
    }
}