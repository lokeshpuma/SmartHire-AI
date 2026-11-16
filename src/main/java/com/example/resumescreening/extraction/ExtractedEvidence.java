package com.example.resumescreening.extraction;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "extracted_evidence")
public class ExtractedEvidence {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "resume_profile_id", nullable = false)
    private ResumeProfile resumeProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false)
    private FieldType fieldType;

    @Column(name = "canonical_value", nullable = false)
    private String canonicalValue;

    @Column(name = "original_text", columnDefinition = "TEXT")
    private String originalText;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "text_offset")
    private Integer textOffset;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_method", nullable = false)
    private ExtractionMethod extractionMethod;

    @Column(nullable = false)
    private double confidence = 1.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false)
    private EvidenceReviewStatus reviewStatus;

    private String notes;

    public ExtractedEvidence() {
        this.id = UUID.randomUUID();
        this.extractionMethod = ExtractionMethod.RULE_BASED;
        this.reviewStatus = EvidenceReviewStatus.UNVERIFIED;
        this.confidence = 1.0;
    }

    public ExtractedEvidence(ResumeProfile resumeProfile, FieldType fieldType, String canonicalValue, String originalText, Integer pageNumber, Integer textOffset) {
        this();
        this.resumeProfile = resumeProfile;
        this.fieldType = fieldType;
        this.canonicalValue = canonicalValue;
        this.originalText = originalText;
        this.pageNumber = pageNumber;
        this.textOffset = textOffset;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ResumeProfile getResumeProfile() { return resumeProfile; }
    public void setResumeProfile(ResumeProfile resumeProfile) { this.resumeProfile = resumeProfile; }

    public FieldType getFieldType() { return fieldType; }
    public void setFieldType(FieldType fieldType) { this.fieldType = fieldType; }

    public String getCanonicalValue() { return canonicalValue; }
    public void setCanonicalValue(String canonicalValue) { this.canonicalValue = canonicalValue; }

    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }

    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

    public Integer getTextOffset() { return textOffset; }
    public void setTextOffset(Integer textOffset) { this.textOffset = textOffset; }

    public ExtractionMethod getExtractionMethod() { return extractionMethod; }
    public void setExtractionMethod(ExtractionMethod extractionMethod) { this.extractionMethod = extractionMethod; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public EvidenceReviewStatus getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(EvidenceReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
