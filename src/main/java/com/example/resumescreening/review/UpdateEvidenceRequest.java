package com.example.resumescreening.review;

import com.example.resumescreening.extraction.EvidenceReviewStatus;

public class UpdateEvidenceRequest {
    private String canonicalValue;
    private String originalText;
    private EvidenceReviewStatus reviewStatus;
    private String notes;

    public UpdateEvidenceRequest() {}

    public String getCanonicalValue() { return canonicalValue; }
    public void setCanonicalValue(String canonicalValue) { this.canonicalValue = canonicalValue; }

    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }

    public EvidenceReviewStatus getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(EvidenceReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
