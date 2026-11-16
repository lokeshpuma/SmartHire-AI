package com.example.resumescreening.extraction;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ResumeProfileDto {
    private UUID id;
    private UUID candidateId;
    private String candidateReference;
    private String displayName;
    private UUID resumeDocumentId;
    private String originalFilename;
    private String profileJson;
    private String extractionVersion;
    private String classificationLabel;
    private double classificationConfidence;
    private ProfileReviewStatus reviewStatus;
    private List<ExtractedEvidenceDto> evidenceItems;
    private Instant createdAt;

    public ResumeProfileDto() {}

    public static ResumeProfileDto fromEntity(ResumeProfile profile) {
        ResumeProfileDto dto = new ResumeProfileDto();
        dto.setId(profile.getId());
        dto.setCandidateId(profile.getCandidate().getId());
        dto.setCandidateReference(profile.getCandidate().getCandidateReference());
        dto.setDisplayName(profile.getCandidate().getDisplayName());
        dto.setResumeDocumentId(profile.getResumeDocument().getId());
        dto.setOriginalFilename(profile.getResumeDocument().getOriginalFilename());
        dto.setProfileJson(profile.getProfileJson());
        dto.setExtractionVersion(profile.getExtractionVersion());
        dto.setClassificationLabel(profile.getClassificationLabel());
        dto.setClassificationConfidence(profile.getClassificationConfidence());
        dto.setReviewStatus(profile.getReviewStatus());
        if (profile.getEvidenceItems() != null) {
            dto.setEvidenceItems(profile.getEvidenceItems().stream()
                    .map(ExtractedEvidenceDto::fromEntity)
                    .collect(Collectors.toList()));
        }
        dto.setCreatedAt(profile.getCreatedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCandidateId() { return candidateId; }
    public void setCandidateId(UUID candidateId) { this.candidateId = candidateId; }

    public String getCandidateReference() { return candidateReference; }
    public void setCandidateReference(String candidateReference) { this.candidateReference = candidateReference; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public UUID getResumeDocumentId() { return resumeDocumentId; }
    public void setResumeDocumentId(UUID resumeDocumentId) { this.resumeDocumentId = resumeDocumentId; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getProfileJson() { return profileJson; }
    public void setProfileJson(String profileJson) { this.profileJson = profileJson; }

    public String getExtractionVersion() { return extractionVersion; }
    public void setExtractionVersion(String extractionVersion) { this.extractionVersion = extractionVersion; }

    public String getClassificationLabel() { return classificationLabel; }
    public void setClassificationLabel(String classificationLabel) { this.classificationLabel = classificationLabel; }

    public double getClassificationConfidence() { return classificationConfidence; }
    public void setClassificationConfidence(double classificationConfidence) { this.classificationConfidence = classificationConfidence; }

    public ProfileReviewStatus getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(ProfileReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }

    public List<ExtractedEvidenceDto> getEvidenceItems() { return evidenceItems; }
    public void setEvidenceItems(List<ExtractedEvidenceDto> evidenceItems) { this.evidenceItems = evidenceItems; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
