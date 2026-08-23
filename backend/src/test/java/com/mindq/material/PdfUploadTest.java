package com.mindq.material;

import com.mindq.config.DotenvInitializer;
import com.mindq.model.User;
import com.mindq.repository.UserRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = DotenvInitializer.class)
@Transactional
class PdfUploadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String UPLOAD_URL = "/api/v1/materials/upload";
    private static final String MATERIALS_URL = "/api/v1/materials";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void shouldUploadPdfAndExtractText() throws Exception {
        String token = registerAndLogin("pdf-upload@example.com");
        byte[] pdfBytes = createSamplePdf("Java Collections", "ArrayList implements List.");

        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.pdf", "application/pdf", pdfBytes);

        MvcResult result = mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .param("title", "My PDF Notes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("My PDF Notes"))
                .andExpect(jsonPath("$.data.materialType").value("PDF_UPLOAD"))
                .andExpect(jsonPath("$.data.fileName").value("notes.pdf"))
                .andExpect(jsonPath("$.data.fileSizeBytes").value((int) pdfBytes.length))
                .andExpect(jsonPath("$.data.wordCount").value(4))
                .andReturn();

        // Verify extracted content using contains (PDFBox trailing whitespace varies)
        String content = MAPPER.readTree(result.getResponse().getContentAsString())
                .path("data").path("content").asText();
        assertTrue(content.contains("Java Collections"));
        assertTrue(content.contains("ArrayList implements List."));
    }

    @Test
    void shouldUseFilenameAsDefaultTitle() throws Exception {
        String token = registerAndLogin("pdf-default-title@example.com");
        byte[] pdfBytes = createSamplePdf("Title", "Body text.");

        MockMultipartFile file = new MockMultipartFile(
                "file", "lecture-notes.pdf", "application/pdf", pdfBytes);

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("lecture-notes"));
    }

    @Test
    void shouldRejectNonPdfExtension() throws Exception {
        String token = registerAndLogin("pdf-wrong-ext@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "application/pdf", "not a pdf".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only PDF and DOCX files are allowed"));
    }

    @Test
    void shouldRejectWrongContentType() throws Exception {
        String token = registerAndLogin("pdf-wrong-type@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.pdf", "text/plain", "not a pdf".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only PDF and DOCX files are allowed"));
    }

    @Test
    void shouldRejectEmptyFile() throws Exception {
        String token = registerAndLogin("pdf-empty@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A file is required"));
    }

    @Test
    void shouldRejectInvalidPdfBytes() throws Exception {
        String token = registerAndLogin("pdf-invalid@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.pdf", "application/pdf",
                "This is not a real PDF file".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Failed to extract text from file"));
    }

    @Test
    void shouldRejectImageOnlyPdf() throws Exception {
        String token = registerAndLogin("pdf-image-only@example.com");
        byte[] imageOnlyPdf = createImageOnlyPdf();

        MockMultipartFile file = new MockMultipartFile(
                "file", "scanned.pdf", "application/pdf", imageOnlyPdf);

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No text could be extracted from the file"));
    }

    @Test
    void shouldRequireAuthForUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "data".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart(UPLOAD_URL).file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadedPdfShouldAppearInListAndDetail() throws Exception {
        String token = registerAndLogin("pdf-roundtrip@example.com");
        byte[] pdfBytes = createSamplePdf("Round Trip", "Verify list and detail.");

        MockMultipartFile file = new MockMultipartFile(
                "file", "roundtrip.pdf", "application/pdf", pdfBytes);

        // Upload
        MvcResult result = mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .param("title", "Round Trip Test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = MAPPER.readTree(result.getResponse().getContentAsString());
        long id = root.path("data").path("id").asLong();

        // List — summary without content
        mockMvc.perform(get(MATERIALS_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Round Trip Test"))
                .andExpect(jsonPath("$.data.content[0].materialType").value("PDF_UPLOAD"));

        // Detail — verify metadata and content
        MvcResult detail = mockMvc.perform(get(MATERIALS_URL + "/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("roundtrip.pdf"))
                .andReturn();

        String content = MAPPER.readTree(detail.getResponse().getContentAsString())
                .path("data").path("content").asText();
        assertTrue(content.contains("Round Trip"));
        assertTrue(content.contains("Verify list and detail."));
    }

    // ---------- helpers ----------

    private byte[] createSamplePdf(String title, String body) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                cs.newLineAtOffset(50, 750);
                cs.showText(title);
                cs.endText();
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(body);
                cs.endText();
            }
            doc.save(baos);
        }
        return baos.toByteArray();
    }

    /**
     * Creates a PDF with one page that contains only a rectangle (no text).
     * PDFTextStripper.getText() returns an empty string for this.
     */
    private byte[] createImageOnlyPdf() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setNonStrokingColor(new Color(200, 200, 200));
                cs.addRect(50, 500, 200, 100);
                cs.fill();
            }
            doc.save(baos);
        }
        return baos.toByteArray();
    }

    private String registerAndLogin(String email) throws Exception {
        userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .fullName("PDF Test User")
                .build());

        String responseBody = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(LOGIN_URL)
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "%s",
                                          "password": "password123"
                                        }
                                        """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return MAPPER.readTree(responseBody).path("data").path("token").asText();
    }
}
