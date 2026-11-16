package com.example.resumescreening.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);
    private final Path uploadDir;

    public StorageService(@Value("${app.storage.upload-dir:./storage/resumes}") String uploadDirPath) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
            log.info("Initialized private resume storage directory at {}", this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory at " + this.uploadDir, e);
        }
    }

    /**
     * Stores an input stream safely using an opaque UUID key.
     * Never trusts user-supplied filenames for disk storage path.
     */
    public String store(InputStream inputStream, String extension) throws IOException {
        String cleanExtension = sanitizeExtension(extension);
        String storageKey = UUID.randomUUID() + cleanExtension;
        Path targetPath = this.uploadDir.resolve(storageKey).normalize();

        // Prevent path traversal
        if (!targetPath.startsWith(this.uploadDir)) {
            throw new SecurityException("Cannot store file outside current storage directory");
        }

        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        return storageKey;
    }

    public InputStream load(String storageKey) throws IOException {
        Path filePath = this.uploadDir.resolve(storageKey).normalize();
        if (!filePath.startsWith(this.uploadDir) || !Files.exists(filePath)) {
            throw new FileNotFoundException("Resume document file not found");
        }
        return Files.newInputStream(filePath);
    }

    public boolean delete(String storageKey) {
        try {
            Path filePath = this.uploadDir.resolve(storageKey).normalize();
            if (filePath.startsWith(this.uploadDir)) {
                return Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete file with storageKey {}: {}", storageKey, e.getMessage());
        }
        return false;
    }

    private String sanitizeExtension(String extension) {
        if (extension == null || extension.isBlank()) return ".bin";
        String ext = extension.trim().toLowerCase();
        if (!ext.startsWith(".")) {
            ext = "." + ext;
        }
        if (ext.equals(".pdf") || ext.equals(".docx") || ext.equals(".txt")) {
            return ext;
        }
        return ".bin";
    }
}
