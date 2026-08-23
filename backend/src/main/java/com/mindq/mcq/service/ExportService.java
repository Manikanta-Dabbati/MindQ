package com.mindq.mcq.service;

import com.mindq.model.McqSet;
import com.mindq.model.Question;

import java.util.List;

/**
 * Abstraction for exporting quiz content to various formats.
 * Currently supports PDF. Future: DOCX, TXT, JSON.
 */
public interface ExportService {

    /**
     * Generate export bytes for a quiz.
     */
    byte[] exportQuiz(McqSet mcqSet, List<Question> questions, String userEmail);

    /**
     * Get the file extension for this export format (e.g., "pdf").
     */
    String getFileExtension();

    /**
     * Get the MIME type for this export format.
     */
    String getContentType();

    /**
     * Get the format name for the Content-Disposition filename.
     */
    String getFormatName();
}
