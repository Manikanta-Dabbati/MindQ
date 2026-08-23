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
public class RevisionNotesResponse {
    private String title;
    private List<NoteSection> sections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NoteSection {
        private String heading;
        private List<String> points;
    }
}
