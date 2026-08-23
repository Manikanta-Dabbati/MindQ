package com.mindq.mcq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Internal DTO representing the AI's full JSON response.
 * Maps to: { "questions": [ ... ] }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedMcqSet {

    private List<GeneratedQuestion> questions;
}
