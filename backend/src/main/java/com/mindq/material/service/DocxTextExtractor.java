package com.mindq.material.service;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Component
public class DocxTextExtractor {

    public String extract(byte[] docxBytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(docxBytes);
             XWPFDocument document = new XWPFDocument(bais);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }
}
