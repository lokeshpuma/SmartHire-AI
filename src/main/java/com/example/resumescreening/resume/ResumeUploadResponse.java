package com.example.resumescreening.resume;

import java.util.UUID;

public class ResumeUploadResponse {
    private boolean success;
    private String filename;
    private UUID documentId;
    private UUID candidateId;
    private String candidateReference;
    private String message;
    private String fileHash;

    public ResumeUploadResponse() {}

    public ResumeUploadResponse(boolean success, String filename, UUID documentId, UUID candidateId, String candidateReference, String message, String fileHash) {
        this.success = success;
        this.filename = filename;
        this.documentId = documentId;
        this.candidateId = candidateId;
        this.candidateReference = candidateReference;
        this.message = message;
        this.fileHash = fileHash;
    }

    public static ResumeUploadResponse success(String filename, UUID documentId, UUID candidateId, String candidateReference, String fileHash) {
        return new ResumeUploadResponse(true, filename, documentId, candidateId, candidateReference, "Resume uploaded and processed successfully", fileHash);
    }

    public static ResumeUploadResponse failed(String filename, String reason) {
        return new ResumeUploadResponse(false, filename, null, null, null, reason, null);
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }

    public UUID getCandidateId() { return candidateId; }
    public void setCandidateId(UUID candidateId) { this.candidateId = candidateId; }

    public String getCandidateReference() { return candidateReference; }
    public void setCandidateReference(String candidateReference) { this.candidateReference = candidateReference; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
}
