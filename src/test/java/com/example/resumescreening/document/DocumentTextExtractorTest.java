package com.example.resumescreening.document;

import com.example.resumescreening.common.ValidationException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTextExtractorTest {

    private DocumentTextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new DocumentTextExtractor(25);
    }

    @Test
    @DisplayName("Extracts plain text document with page marker")
    void testExtractPlainText() {
        String content = "Senior Java Engineer with 10 years experience building distributed backend systems.";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        DocumentTextExtractor.ExtractionResult result = extractor.extractText(bytes, "text/plain", "resume.txt");

        assertNotNull(result);
        assertEquals(1, result.getPageCount());
        assertTrue(result.getNormalizedFullText().contains("Senior Java Engineer"));
        assertTrue(result.getNormalizedFullText().contains("--- [Page 1] ---"));
    }

    @Test
    @DisplayName("Extracts generated PDF document with page numbers preserved")
    void testExtractPdfText() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText("Software Engineer proficient in Spring Boot and Docker.");
                contentStream.endText();
            }
            doc.save(baos);
        }

        byte[] pdfBytes = baos.toByteArray();
        DocumentTextExtractor.ExtractionResult result = extractor.extractText(pdfBytes, "application/pdf", "sample.pdf");

        assertNotNull(result);
        assertEquals(1, result.getPageCount());
        assertTrue(result.getNormalizedFullText().contains("Software Engineer proficient in Spring Boot"));
        assertTrue(result.getNormalizedFullText().contains("--- [Page 1] ---"));
    }

    @Test
    @DisplayName("Detects and rejects empty or low-quality extraction (< 30 characters)")
    void testRejectLowQualityExtraction() {
        String tiny = "Tiny text";
        byte[] bytes = tiny.getBytes(StandardCharsets.UTF_8);

        assertThrows(ValidationException.class, () -> extractor.extractText(bytes, "text/plain", "tiny.txt"));
    }
}
