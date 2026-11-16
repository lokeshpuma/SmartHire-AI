package com.example.resumescreening.document;

import com.example.resumescreening.common.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class DocumentValidator {

    private final long maxFileSizeBytes;

    public DocumentValidator(@Value("${app.storage.max-file-size-bytes:15728640}") long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public ValidationResult validateAndHash(String originalFilename, byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new ValidationException("Uploaded file is empty");
        }

        if (fileBytes.length > maxFileSizeBytes) {
            throw new ValidationException("File size exceeds limit of " + (maxFileSizeBytes / (1024 * 1024)) + "MB");
        }

        String extension = extractExtension(originalFilename);
        String detectedMimeType = detectMimeType(fileBytes, extension);

        if (detectedMimeType == null) {
            throw new ValidationException("Unsupported document type or signature mismatch for file: " + originalFilename + ". Allowed: PDF, DOCX, TXT");
        }

        String sha256 = calculateSha256(fileBytes);
        return new ValidationResult(originalFilename, extension, detectedMimeType, sha256, fileBytes.length);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.')).toLowerCase();
    }

    private String detectMimeType(byte[] bytes, String extension) {
        // PDF Magic Bytes: %PDF- (0x25 0x50 0x44 0x46 0x2D)
        if (bytes.length >= 5 && bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46 && bytes[4] == 0x2D) {
            return "application/pdf";
        }

        // DOCX (ZIP archive header): PK\x03\x04 (0x50 0x4B 0x03 0x04)
        if (bytes.length >= 4 && bytes[0] == 0x50 && bytes[1] == 0x4B && bytes[2] == 0x03 && bytes[3] == 0x04) {
            if (".docx".equalsIgnoreCase(extension)) {
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            }
        }

        // TXT: Must have .txt extension and contain primarily printable ASCII/UTF-8 characters
        if (".txt".equalsIgnoreCase(extension)) {
            if (isTextFile(bytes)) {
                return "text/plain";
            }
        }

        return null;
    }

    private boolean isTextFile(byte[] bytes) {
        int sampleLength = Math.min(bytes.length, 1024);
        int nonPrintable = 0;
        for (int i = 0; i < sampleLength; i++) {
            byte b = bytes[i];
            if (b == 0) return false; // Null byte indicates binary
            if ((b < 32 || b > 126) && b != 9 && b != 10 && b != 13) {
                nonPrintable++;
            }
        }
        return (double) nonPrintable / sampleLength < 0.05;
    }

    private String calculateSha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public static class ValidationResult {
        private final String originalFilename;
        private final String extension;
        private final String mimeType;
        private final String sha256;
        private final long sizeBytes;

        public ValidationResult(String originalFilename, String extension, String mimeType, String sha256, long sizeBytes) {
            this.originalFilename = originalFilename;
            this.extension = extension;
            this.mimeType = mimeType;
            this.sha256 = sha256;
            this.sizeBytes = sizeBytes;
        }

        public String getOriginalFilename() { return originalFilename; }
        public String getExtension() { return extension; }
        public String getMimeType() { return mimeType; }
        public String getSha256() { return sha256; }
        public long getSizeBytes() { return sizeBytes; }
    }
}
