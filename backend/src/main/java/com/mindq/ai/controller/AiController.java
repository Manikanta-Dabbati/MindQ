package com.mindq.ai.controller;

import com.mindq.common.api.ApiResponse;
import com.mindq.model.AIModel;
import com.mindq.repository.AIModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AIModelRepository aiModelRepository;

    @GetMapping("/models")
    public ResponseEntity<ApiResponse<List<AIModel>>> getModels() {
        List<AIModel> models = aiModelRepository.findByIsActiveTrue();
        return ResponseEntity.ok(ApiResponse.success(models, "AI models retrieved"));
    }
}
