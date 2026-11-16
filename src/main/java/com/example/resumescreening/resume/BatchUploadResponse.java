package com.example.resumescreening.resume;

import java.util.List;

public class BatchUploadResponse {
    private int totalFiles;
    private int successCount;
    private int failureCount;
    private List<ResumeUploadResponse> results;

    public BatchUploadResponse() {}

    public BatchUploadResponse(int totalFiles, int successCount, int failureCount, List<ResumeUploadResponse> results) {
        this.totalFiles = totalFiles;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.results = results;
    }

    public int getTotalFiles() { return totalFiles; }
    public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getFailureCount() { return failureCount; }
    public void setFailureCount(int failureCount) { this.failureCount = failureCount; }

    public List<ResumeUploadResponse> getResults() { return results; }
    public void setResults(List<ResumeUploadResponse> results) { this.results = results; }
}
