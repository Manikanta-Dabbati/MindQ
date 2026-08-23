package com.mindq.ai.controller;

import com.mindq.ai.dto.*;
import com.mindq.ai.service.AiToolService;
import com.mindq.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/tools")
@RequiredArgsConstructor
public class AiToolController {

    private final AiToolService aiToolService;

    @PostMapping("/summarize")
    public ResponseEntity<ApiResponse<SummaryResponse>> summarize(
            @Valid @RequestBody AiToolRequest request,
            Authentication authentication) {
        SummaryResponse response = aiToolService.summarize(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Summary generated successfully"));
    }

    @PostMapping("/flashcards")
    public ResponseEntity<ApiResponse<FlashcardResponse>> generateFlashcards(
            @Valid @RequestBody AiToolRequest request,
            Authentication authentication) {
        FlashcardResponse response = aiToolService.generateFlashcards(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Flashcards generated successfully"));
    }

    @PostMapping("/revision-notes")
    public ResponseEntity<ApiResponse<RevisionNotesResponse>> generateRevisionNotes(
            @Valid @RequestBody AiToolRequest request,
            Authentication authentication) {
        RevisionNotesResponse response = aiToolService.generateRevisionNotes(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Revision notes generated successfully"));
    }
}
