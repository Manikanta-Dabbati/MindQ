package com.mindq.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResult {
    private Long id;
    private String type;       // "MATERIAL" or "QUIZ"
    private String title;
    private String subtitle;   // e.g. "PDF · 1200 words" or "Easy · 20 questions"
    private String link;       // e.g. "/vault" or "/quiz/5"
    private String icon;       // "file" or "quiz"
}
