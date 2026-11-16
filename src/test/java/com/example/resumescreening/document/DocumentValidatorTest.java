package com.example.resumescreening.document;

import com.example.resumescreening.common.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DocumentValidatorTest {

    private DocumentValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DocumentValidator(15 * 1024 * 1024); // 15MB
    }

    @Test
    @DisplayName("Validates plain text file successfully")
    void testValidatePlainText() {
        String content = "Senior Java Engineer with 5 years experience in Spring Boot and PostgreSQL.";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        DocumentValidator.ValidationResult result = validator.validateAndHash("resume.txt", bytes);

        assertNotNull(result);
        assertEquals(".txt", result.getExtension());
        assertEquals("text/plain", result.getMimeType());
        assertNotNull(result.getSha256());
        assertEquals(64, result.getSha256().length());
    }

    @Test
    @DisplayName("Validates PDF file signature (%PDF-)")
    void testValidatePdfMagicBytes() {
        byte[] pdfBytes = "%PDF-1.7\nSample PDF resume content".getBytes(StandardCharsets.UTF_8);

        DocumentValidator.ValidationResult result = validator.validateAndHash("resume.pdf", pdfBytes);

        assertEquals(".pdf", result.getExtension());
        assertEquals("application/pdf", result.getMimeType());
    }

    @Test
    @DisplayName("Validates DOCX file signature (PK header)")
    void testValidateDocxMagicBytes() {
        byte[] docxBytes = new byte[] {0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x06, 0x00};

        DocumentValidator.ValidationResult result = validator.validateAndHash("resume.docx", docxBytes);

        assertEquals(".docx", result.getExtension());
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", result.getMimeType());
    }

    @Test
    @DisplayName("Rejects unsupported file format with clear error")
    void testRejectUnsupportedFormat() {
        byte[] exeBytes = new byte[] {0x4D, 0x5A, 0x00, 0x03, 0x00, 0x00};

        assertThrows(ValidationException.class, () -> validator.validateAndHash("resume.exe", exeBytes));
    }

    @Test
    @DisplayName("Rejects empty document")
    void testRejectEmptyDocument() {
        assertThrows(ValidationException.class, () -> validator.validateAndHash("resume.pdf", new byte[0]));
    }

    @Test
    @DisplayName("Rejects oversized file exceeding maximum size limit")
    void testRejectOversizedDocument() {
        DocumentValidator tinyValidator = new DocumentValidator(100); // 100 bytes limit
        byte[] bigBytes = new byte[200];

        assertThrows(ValidationException.class, () -> tinyValidator.validateAndHash("resume.pdf", bigBytes));
    }
}
