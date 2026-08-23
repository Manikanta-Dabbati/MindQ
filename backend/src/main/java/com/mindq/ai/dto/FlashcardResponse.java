package com.mindq.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardResponse {
    private Long setId;
    private String title;
    private List<FlashcardItem> flashcards;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlashcardItem {
        private String front;
        private String back;
    }
}
