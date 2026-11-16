package com.example.resumescreening.extraction;

import java.util.UUID;

public class ExtractedEvidenceDto {
    private UUID id;
    private FieldType fieldType;
    private String canonicalValue;
    private String originalText;
    private Integer pageNumber;
    private Integer textOffset;
    private ExtractionMethod extractionMethod;
    private double confidence;
    private EvidenceReviewStatus reviewStatus;
    private String notes;

    public ExtractedEvidenceDto() {}

    public static ExtractedEvidenceDto fromEntity(ExtractedEvidence ev) {
        ExtractedEvidenceDto dto = new ExtractedEvidenceDto();
        dto.setId(ev.getId());
        dto.setFieldType(ev.getFieldType());
        dto.setCanonicalValue(ev.getCanonicalValue());
        dto.setOriginalText(ev.getOriginalText());
        dto.setPageNumber(ev.getPageNumber());
        dto.setTextOffset(ev.getTextOffset());
        dto.setExtractionMethod(ev.getExtractionMethod());
        dto.setConfidence(ev.getConfidence());
        dto.setReviewStatus(ev.getReviewStatus());
        dto.setNotes(ev.getNotes());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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
