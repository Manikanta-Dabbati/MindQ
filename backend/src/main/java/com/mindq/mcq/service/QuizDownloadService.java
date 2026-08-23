package com.mindq.mcq.service;

import com.mindq.material.exception.MaterialNotFoundException;
import com.mindq.model.McqSet;
import com.mindq.model.Question;
import com.mindq.model.User;
import com.mindq.repository.McqSetRepository;
import com.mindq.repository.QuestionRepository;
import com.mindq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizDownloadService {

    private final McqSetRepository mcqSetRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final PdfExportService pdfExportService;

    /**
     * Generate a real PDF for the quiz.
     */
    @Transactional(readOnly = true)
    public byte[] generatePdf(Long mcqSetId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new MaterialNotFoundException("User not found"));

        McqSet mcqSet = mcqSetRepository.findById(mcqSetId)
                .orElseThrow(() -> new MaterialNotFoundException("MCQ set not found"));

        if (!mcqSet.getUser().getId().equals(user.getId())) {
            throw new MaterialNotFoundException("MCQ set not found");
        }

        List<Question> questions = questionRepository.findByMcqSetIdWithOptions(mcqSetId);

        log.info("Generating PDF for quiz '{}' ({} questions) for user {}",
                mcqSet.getTitle(), questions.size(), userEmail);

        return pdfExportService.exportQuiz(mcqSet, questions, userEmail);
    }

    /**
     * Get the filename for download.
     */
    @Transactional(readOnly = true)
    public String generateFilename(Long mcqSetId, String userEmail) {
        McqSet mcqSet = mcqSetRepository.findById(mcqSetId)
                .orElseThrow(() -> new MaterialNotFoundException("MCQ set not found"));

        String safe = mcqSet.getTitle().replaceAll("[^a-zA-Z0-9\\s-]", "").replaceAll("\\s+", "_");
        return safe + "." + pdfExportService.getFileExtension();
    }
}
