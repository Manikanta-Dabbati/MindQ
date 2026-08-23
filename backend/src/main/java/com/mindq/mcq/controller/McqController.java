package com.mindq.mcq.controller;

import com.mindq.common.api.ApiResponse;
import com.mindq.material.dto.MaterialDetailResponse;
import com.mindq.mcq.dto.*;
import com.mindq.mcq.service.McqGeneratorService;
import com.mindq.mcq.service.McqService;
import com.mindq.mcq.service.QuizDownloadService;
import com.mindq.material.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mcq")
@RequiredArgsConstructor
public class McqController {

    private final McqGeneratorService mcqGeneratorService;
    private final McqService mcqService;
    private final QuizDownloadService quizDownloadService;
    private final MaterialService materialService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<McqSetResponse>> generate(
            @Valid @RequestBody GenerateMcqRequest request,
            Authentication authentication) {
        McqSetResponse response = mcqGeneratorService.generate(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "MCQ set generated successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<McqSetResponse>> getMcqSet(
            @PathVariable Long id,
            Authentication authentication) {
        McqSetResponse response = mcqService.getMcqSet(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success(response, "MCQ set retrieved successfully"));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<QuizResultResponse>> submitQuiz(
            @PathVariable Long id,
            @Valid @RequestBody SubmitQuizRequest request,
            Authentication authentication) {
        QuizResultResponse response = mcqService.submitQuiz(authentication.getName(), id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Quiz submitted successfully"));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<QuizHistoryResponse>>> getQuizHistory(
            Authentication authentication) {
        List<QuizHistoryResponse> history = mcqService.getQuizHistory(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(history, "Quiz history retrieved successfully"));
    }

    @GetMapping("/attempt/{attemptId}/answers")
    public ResponseEntity<ApiResponse<List<QuizAnswerResponse>>> getAttemptAnswers(
            @PathVariable Long attemptId,
            Authentication authentication) {
        List<QuizAnswerResponse> answers = mcqService.getAttemptAnswers(authentication.getName(), attemptId);
        return ResponseEntity.ok(ApiResponse.success(answers, "Attempt answers retrieved successfully"));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadQuiz(
            @PathVariable Long id,
            Authentication authentication) {
        byte[] content = quizDownloadService.generatePdf(id, authentication.getName());
        String filename = quizDownloadService.generateFilename(id, authentication.getName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(content.length)
                .body(content);
    }

    @PostMapping("/{id}/save-to-vault")
    public ResponseEntity<ApiResponse<MaterialDetailResponse>> saveToVault(
            @PathVariable Long id,
            Authentication authentication) {
        MaterialDetailResponse material = mcqService.saveToVault(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success(material, "Quiz saved to Knowledge Vault"));
    }
}
