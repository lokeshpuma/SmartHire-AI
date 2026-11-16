package com.example.resumescreening.document;

import com.example.resumescreening.common.ValidationException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(DocumentTextExtractor.class);
    private static final String PARSER_VERSION = "Tika-PDFBox3-POI5";

    private final int maxPages;

    public DocumentTextExtractor(@Value("${app.storage.max-pages:25}") int maxPages) {
        this.maxPages = maxPages;
    }

    public ExtractionResult extractText(byte[] fileBytes, String mimeType, String filename) {
        try {
            if ("application/pdf".equalsIgnoreCase(mimeType)) {
                return extractPdfText(fileBytes, filename);
            } else if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equalsIgnoreCase(mimeType)) {
                return extractDocxText(fileBytes, filename);
            } else if ("text/plain".equalsIgnoreCase(mimeType)) {
                return extractPlainText(fileBytes, filename);
            } else {
                throw new ValidationException("Unsupported MIME type: " + mimeType);
            }
        } catch (InvalidPasswordException e) {
            throw new ValidationException("Password-protected or encrypted document cannot be parsed: " + filename);
        } catch (IOException e) {
            log.error("Failed to extract text from {}: {}", filename, e.getMessage());
            throw new ValidationException("Document text extraction failed: " + e.getMessage());
        }
    }

    private ExtractionResult extractPdfText(byte[] bytes, String filename) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                throw new ValidationException("PDF document is encrypted or password-protected: " + filename);
            }

            int pageCount = document.getNumberOfPages();
            if (pageCount > maxPages) {
                throw new ValidationException("Document exceeds maximum allowed page count (" + maxPages + " pages)");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            StringBuilder fullText = new StringBuilder();
            List<PageText> pages = new ArrayList<>();

            for (int p = 1; p <= pageCount; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String pageContent = stripper.getText(document);
                String normalized = normalizeWhitespace(pageContent);
                pages.add(new PageText(p, normalized));

                fullText.append("\n--- [Page ").append(p).append("] ---\n");
                fullText.append(normalized).append("\n");
            }

            String finalNormalizedText = fullText.toString().trim();
            validateExtractionQuality(finalNormalizedText, filename);

            return new ExtractionResult(
                    finalNormalizedText,
                    pages,
                    pageCount,
                    "Apache-PDFBox-3.0",
                    PARSER_VERSION
            );
        }
    }

    private ExtractionResult extractDocxText(byte[] bytes, String filename) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             XWPFDocument docx = new XWPFDocument(bis)) {

            StringBuilder fullText = new StringBuilder();
            List<PageText> pages = new ArrayList<>();
            StringBuilder currentPage = new StringBuilder();
            int pageNumber = 1;

            for (XWPFParagraph p : docx.getParagraphs()) {
                String text = p.getText();
                if (text != null && !text.isBlank()) {
                    currentPage.append(text).append("\n");
                }
            }

            String normalized = normalizeWhitespace(currentPage.toString());
            pages.add(new PageText(pageNumber, normalized));
            fullText.append("--- [Page 1] ---\n").append(normalized);

            String finalNormalizedText = fullText.toString().trim();
            validateExtractionQuality(finalNormalizedText, filename);

            return new ExtractionResult(
                    finalNormalizedText,
                    pages,
                    1,
                    "Apache-POI-5.3",
                    PARSER_VERSION
            );
        }
    }

    private ExtractionResult extractPlainText(byte[] bytes, String filename) {
        String rawText = new String(bytes, StandardCharsets.UTF_8);
        String normalized = normalizeWhitespace(rawText);
        validateExtractionQuality(normalized, filename);

        List<PageText> pages = List.of(new PageText(1, normalized));
        String marked = "--- [Page 1] ---\n" + normalized;

        return new ExtractionResult(
                marked,
                pages,
                1,
                "PlainTextReader",
                PARSER_VERSION
        );
    }

    private void validateExtractionQuality(String text, String filename) {
        if (text == null || text.trim().length() < 30) {
            throw new ValidationException("Low quality or empty extraction. Document contains insufficient legible text: " + filename);
        }
    }

    private String normalizeWhitespace(String text) {
        if (text == null) return "";
        // Normalize CRLF to LF, collapse repeated tabs and spaces without removing paragraph newlines
        return text.replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    public static class PageText {
        private final int pageNumber;
        private final String text;

        public PageText(int pageNumber, String text) {
            this.pageNumber = pageNumber;
            this.text = text;
        }

        public int getPageNumber() { return pageNumber; }
        public String getText() { return text; }
    }

    public static class ExtractionResult {
        private final String normalizedFullText;
        private final List<PageText> pages;
        private final int pageCount;
        private final String parserName;
        private final String parserVersion;

        public ExtractionResult(String normalizedFullText, List<PageText> pages, int pageCount, String parserName, String parserVersion) {
            this.normalizedFullText = normalizedFullText;
            this.pages = pages;
            this.pageCount = pageCount;
            this.parserName = parserName;
            this.parserVersion = parserVersion;
        }

        public String getNormalizedFullText() { return normalizedFullText; }
        public List<PageText> getPages() { return pages; }
        public int getPageCount() { return pageCount; }
        public String getParserName() { return parserName; }
        public String getParserVersion() { return parserVersion; }
    }
}
