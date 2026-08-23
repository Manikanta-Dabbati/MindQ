package com.mindq.material.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Isolates PDFBox usage so the rest of the code only deals with plain text.
 */
@Component
public class PdfTextExtractor {

    /**
     * Extracts all text from the given PDF bytes.
     * Throws IOException when the bytes are not a readable PDF.
     * Returns an empty/blank string for image-only (scanned) PDFs.
     */
    public String extract(byte[] pdfBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
