package com.mindq.material.service;

import com.mindq.auth.exception.InvalidCredentialsException;
import com.mindq.enums.MaterialType;
import com.mindq.common.api.PaginatedResponse;
import com.mindq.material.dto.CreateMaterialRequest;
import com.mindq.material.dto.MaterialDetailResponse;
import com.mindq.material.dto.MaterialSummaryProjection;
import com.mindq.material.dto.MaterialSummaryResponse;
import com.mindq.material.dto.UpdateMaterialRequest;
import com.mindq.material.exception.InvalidPdfException;
import com.mindq.material.exception.MaterialNotFoundException;
import com.mindq.model.StudyMaterial;
import com.mindq.model.User;
import com.mindq.repository.StudyMaterialRepository;
import com.mindq.repository.UserRepository;
import com.mindq.common.metrics.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private static final int MAX_TITLE_LENGTH = 255;

    private final StudyMaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final PdfTextExtractor pdfTextExtractor;
    private final DocxTextExtractor docxTextExtractor;
    private final StorageService storageService;
    private final MetricsService metricsService;

    @Transactional
    public MaterialDetailResponse create(String email, CreateMaterialRequest request) {
        User user = currentUser(email);

        StudyMaterial material = StudyMaterial.builder()
                .user(user)
                .title(request.getTitle().trim())
                .materialType(MaterialType.TEXT_PASTE)
                .rawText(request.getContent())
                .wordCount(countWords(request.getContent()))
                .build();

        return toDetail(materialRepository.save(material));
    }

    /**
     * List materials with DB-level projection — avoids loading rawText (LONGTEXT).
     * Uses summary-only projection for efficient listing.
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<MaterialSummaryResponse> list(String email, int page, int size, String search) {
        User user = currentUser(email);

        // Use projection query — only loads metadata, not rawText
        List<MaterialSummaryProjection> projections;
        if (search != null && !search.isBlank()) {
            projections = materialRepository.findSummariesByUserIdWithSearch(user.getId(), search.trim());
        } else {
            projections = materialRepository.findSummariesByUserId(user.getId());
        }

        long totalElements = projections.size();

        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, projections.size());
        List<MaterialSummaryResponse> paged = projections.subList(start, end).stream()
                .map(this::projectionToSummary)
                .toList();

        return PaginatedResponse.of(paged, page, size, totalElements);
    }

    @Transactional(readOnly = true)
    public MaterialDetailResponse get(String email, Long id) {
        return toDetail(ownedMaterial(email, id));
    }

    @Transactional
    public MaterialDetailResponse update(String email, Long id, UpdateMaterialRequest request) {
        StudyMaterial material = ownedMaterial(email, id);
        material.setTitle(request.getTitle().trim());
        material.setRawText(request.getContent());
        material.setWordCount(countWords(request.getContent()));

        // Flush now so @UpdateTimestamp refreshes updatedAt before we build the response.
        return toDetail(materialRepository.saveAndFlush(material));
    }

    @Transactional
    public void delete(String email, Long id) {
        materialRepository.delete(ownedMaterial(email, id));
    }

    @Transactional
    public MaterialDetailResponse uploadFile(String email, String title, MultipartFile file) {
        User user = currentUser(email);
        validateFile(file);

        // Check storage quota
        if (!storageService.hasEnoughSpace(email, file.getSize())) {
            throw new InvalidPdfException("Storage limit exceeded. Please delete some materials to free up space.");
        }

        String filename = file.getOriginalFilename();
        boolean isPdf = filename != null && filename.toLowerCase().endsWith(".pdf");
        boolean isDocx = filename != null && filename.toLowerCase().endsWith(".docx");

        String extractedText;
        try {
            if (isPdf) {
                extractedText = pdfTextExtractor.extract(file.getBytes());
            } else {
                extractedText = docxTextExtractor.extract(file.getBytes());
            }
        } catch (IOException ex) {
            throw new InvalidPdfException("Failed to extract text from file");
        }

        if (extractedText == null || extractedText.isBlank()) {
            throw new InvalidPdfException("No text could be extracted from the file");
        }

        MaterialType materialType = isPdf ? MaterialType.PDF_UPLOAD : MaterialType.TEXT_PASTE;
        String resolvedTitle = resolveTitle(title, filename);

        StudyMaterial material = StudyMaterial.builder()
                .user(user)
                .title(resolvedTitle)
                .materialType(materialType)
                .rawText(extractedText)
                .fileName(filename)
                .fileSizeBytes(file.getSize())
                .wordCount(countWords(extractedText))
                .build();

        StudyMaterial saved = materialRepository.save(material);
        metricsService.recordUpload(file.getSize());
        return toDetail(saved);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidPdfException("A file is required");
        }

        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        if (filename == null) {
            throw new InvalidPdfException("File must have a name");
        }

        String lower = filename.toLowerCase();
        boolean hasPdfExt = lower.endsWith(".pdf");
        boolean hasDocxExt = lower.endsWith(".docx");

        if (!hasPdfExt && !hasDocxExt) {
            throw new InvalidPdfException("Only PDF and DOCX files are allowed");
        }

        if (hasPdfExt && contentType != null && !contentType.equalsIgnoreCase("application/pdf")) {
            throw new InvalidPdfException("Only PDF and DOCX files are allowed");
        }

        if (hasDocxExt && contentType != null && !contentType.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                && !contentType.equalsIgnoreCase("application/octet-stream")) {
            throw new InvalidPdfException("Only PDF and DOCX files are allowed");
        }
    }

    private String resolveTitle(String title, String filename) {
        String resolved;
        if (title != null && !title.isBlank()) {
            resolved = title.trim();
        } else {
            resolved = defaultTitle(filename);
        }

        if (resolved.length() > MAX_TITLE_LENGTH) {
            throw new InvalidPdfException("Title must be at most " + MAX_TITLE_LENGTH + " characters");
        }
        return resolved;
    }

    private String defaultTitle(String filename) {
        if (filename == null || filename.isBlank()) {
            return "Untitled";
        }
        String base = filename;
        if (base.toLowerCase().endsWith(".pdf")) {
            base = base.substring(0, base.length() - 4);
        }
        return base.isBlank() ? "Untitled" : base;
    }

    /**
     * Returns the material only if it belongs to the caller.
     * Both "missing" and "owned by someone else" map to 404 so foreign
     * resources are never revealed to other users.
     */
        private StudyMaterial ownedMaterial(String email, Long id) {
        User user = currentUser(email);
        StudyMaterial material = materialRepository.findById(id)
                .orElseThrow(() -> new MaterialNotFoundException("Material not found"));

        if (!material.getUser().getId().equals(user.getId())) {
            throw new MaterialNotFoundException("Material not found");
        }
        return material;
    }

    private User currentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\s+").length;
    }

    private MaterialSummaryResponse projectionToSummary(MaterialSummaryProjection projection) {
        return MaterialSummaryResponse.builder()
                .id(projection.getId())
                .title(projection.getTitle())
                .materialType(projection.getMaterialType())
                .wordCount(projection.getWordCount())
                .createdAt(projection.getCreatedAt())
                .updatedAt(projection.getUpdatedAt())
                .build();
    }

    private MaterialSummaryResponse toSummary(StudyMaterial material) {
        return MaterialSummaryResponse.builder()
                .id(material.getId())
                .title(material.getTitle())
                .materialType(material.getMaterialType())
                .wordCount(material.getWordCount())
                .createdAt(material.getCreatedAt())
                .updatedAt(material.getUpdatedAt())
                .build();
    }

    private MaterialDetailResponse toDetail(StudyMaterial material) {
        return MaterialDetailResponse.builder()
                .id(material.getId())
                .title(material.getTitle())
                .materialType(material.getMaterialType())
                .content(material.getRawText())
                .wordCount(material.getWordCount())
                .fileName(material.getFileName())
                .fileSizeBytes(material.getFileSizeBytes())
                .createdAt(material.getCreatedAt())
                .updatedAt(material.getUpdatedAt())
                .build();
    }
}
