package com.mindq.mcq.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.mindq.model.McqSet;
import com.mindq.model.Question;
import com.mindq.model.QuestionOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates real PDF documents for quizzes using OpenPDF.
 * Professional layout with MindQ branding, proper fonts, and structured content.
 */
@Slf4j
@Component
public class PdfExportService implements ExportService {

    private static final Color PRIMARY_BLUE = new Color(37, 99, 235);    // #2563EB
    private static final Color AI_PURPLE = new Color(124, 58, 237);     // #7C3AED
    private static final Color TEXT_DARK = new Color(15, 23, 42);       // #0F172A
    private static final Color TEXT_SECONDARY = new Color(100, 116, 139); // #64748B
    private static final Color CORRECT_GREEN = new Color(22, 163, 74);  // #16a34a
    private static final Color BORDER_LIGHT = new Color(226, 232, 240); // #E2E8F0
    private static final Color BG_LIGHT = new Color(248, 250, 252);     // #F8FAFC

    @Override
    public byte[] exportQuiz(McqSet mcqSet, List<Question> questions, String userEmail) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(document, baos);

        document.open();

        // --- Title Section ---
        addTitle(document, mcqSet, questions);

        // --- Questions ---
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            addQuestion(document, q, i + 1);
        }

        // --- Footer ---
        addFooter(document, mcqSet);

        document.close();
        return baos.toByteArray();
    }

    private void addTitle(Document document, McqSet mcqSet, List<Question> questions) throws DocumentException {
        // MindQ brand
        Paragraph brand = new Paragraph();
        brand.setAlignment(Element.ALIGN_LEFT);
        Chunk brandChunk = new Chunk("Mind", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, PRIMARY_BLUE));
        brand.add(brandChunk);
        brand.add(new Chunk("Q", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, AI_PURPLE)));
        document.add(brand);

        // Quiz title
        Paragraph title = new Paragraph(mcqSet.getTitle(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, TEXT_DARK));
        title.setSpacingBefore(10);
        title.setSpacingAfter(5);
        document.add(title);

        // Metadata line
        String meta = String.format("Difficulty: %s  |  Questions: %d  |  Generated: %s",
                mcqSet.getDifficulty(),
                questions.size(),
                mcqSet.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
        Paragraph metaPara = new Paragraph(meta,
                FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_SECONDARY));
        metaPara.setSpacingAfter(15);
        document.add(metaPara);

        // Divider line
        PdfPTable dividerTable = new PdfPTable(1);
        dividerTable.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER_LIGHT);
        cell.setBorderWidth(0.5f);
        cell.setBorder(com.lowagie.text.Rectangle.BOTTOM);
        cell.setFixedHeight(1);
        dividerTable.addCell(cell);
        document.add(dividerTable);
        document.add(Chunk.NEWLINE);
    }

    private void addQuestion(Document document, Question q, int number) throws DocumentException {
        // Question number + text
        Font questionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, TEXT_DARK);
        Paragraph questionPara = new Paragraph();
        questionPara.setSpacingBefore(12);
        questionPara.setSpacingAfter(6);

        Chunk numChunk = new Chunk(number + ".  ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PRIMARY_BLUE));
        questionPara.add(numChunk);
        questionPara.add(new Chunk(q.getQuestionText(), questionFont));
        document.add(questionPara);

        // Options
        List<QuestionOption> options = q.getOptions();
        Font optionFont = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_DARK);
        Font correctFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, CORRECT_GREEN);

        for (QuestionOption opt : options) {
            String label = String.valueOf((char) ('A' + opt.getOptionOrder()));
            Paragraph optPara = new Paragraph();
            optPara.setIndentationLeft(20);
            optPara.setSpacingAfter(2);

            Chunk labelChunk;
            if (opt.getIsCorrect()) {
                labelChunk = new Chunk(label + ".  ", correctFont);
            } else {
                labelChunk = new Chunk(label + ".  ", optionFont);
            }
            optPara.add(labelChunk);

            if (opt.getIsCorrect()) {
                optPara.add(new Chunk(opt.getOptionText() + "  ✓", correctFont));
            } else {
                optPara.add(new Chunk(opt.getOptionText(), optionFont));
            }

            document.add(optPara);
        }

        // Explanation
        if (q.getExplanation() != null && !q.getExplanation().isBlank()) {
            Font explanationFont = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_SECONDARY);
            Paragraph explanationPara = new Paragraph();
            explanationPara.setIndentationLeft(20);
            explanationPara.setSpacingBefore(4);
            explanationPara.setSpacingAfter(8);

            Chunk expLabel = new Chunk("Explanation: ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXT_SECONDARY));
            explanationPara.add(expLabel);
            explanationPara.add(new Chunk(q.getExplanation(), explanationFont));
            document.add(explanationPara);
        }
    }

    private void addFooter(Document document, McqSet mcqSet) throws DocumentException {
        document.add(Chunk.NEWLINE);

        PdfPTable dividerTable = new PdfPTable(1);
        dividerTable.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER_LIGHT);
        cell.setBorderWidth(0.5f);
        cell.setBorder(com.lowagie.text.Rectangle.BOTTOM);
        cell.setFixedHeight(1);
        dividerTable.addCell(cell);
        document.add(dividerTable);

        Paragraph footer = new Paragraph();
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(10);

        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_SECONDARY);
        footer.add(new Chunk("Generated by MindQ — Sync Your Mind with AI", footerFont));
        document.add(footer);
    }

    @Override
    public String getFileExtension() {
        return "pdf";
    }

    @Override
    public String getContentType() {
        return "application/pdf";
    }

    @Override
    public String getFormatName() {
        return "PDF";
    }
}
