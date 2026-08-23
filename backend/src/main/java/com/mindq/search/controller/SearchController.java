package com.mindq.search.controller;

import com.mindq.common.api.ApiResponse;
import com.mindq.search.dto.SearchResult;
import com.mindq.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SearchResult>>> search(
            @RequestParam String q,
            Authentication authentication) {
        List<SearchResult> results = searchService.search(authentication.getName(), q);
        return ResponseEntity.ok(ApiResponse.success(results, "Search completed"));
    }
}
