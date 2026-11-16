package com.example.resumescreening.extraction;

import com.example.resumescreening.candidate.Candidate;
import com.example.resumescreening.document.ResumeDocument;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "resume_profiles")
public class ResumeProfile {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "resume_document_id", nullable = false)
    private ResumeDocument resumeDocument;

    @Column(name = "normalized_text", columnDefinition = "TEXT")
    private String normalizedText;

    @Column(name = "profile_json", columnDefinition = "TEXT")
    private String profileJson;

    @Column(name = "extraction_version")
    private String extractionVersion;

    @Column(name = "classification_label")
    private String classificationLabel;

    @Column(name = "classification_confidence")
    private double classificationConfidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false)
    private ProfileReviewStatus reviewStatus;

    @OneToMany(mappedBy = "resumeProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExtractedEvidence> evidenceItems = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ResumeProfile() {
        this.id = UUID.randomUUID();
        this.reviewStatus = ProfileReviewStatus.NOT_REVIEWED;
        this.createdAt = Instant.now();
    }

    public ResumeProfile(Candidate candidate, ResumeDocument resumeDocument, String normalizedText, String profileJson, String extractionVersion) {
        this();
        this.candidate = candidate;
        this.resumeDocument = resumeDocument;
        this.normalizedText = normalizedText;
        this.profileJson = profileJson;
        this.extractionVersion = extractionVersion;
    }

    public void addEvidence(ExtractedEvidence evidence) {
        evidenceItems.add(evidence);
        evidence.setResumeProfile(this);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Candidate getCandidate() { return candidate; }
    public void setCandidate(Candidate candidate) { this.candidate = candidate; }

    public ResumeDocument getResumeDocument() { return resumeDocument; }
    public void setResumeDocument(ResumeDocument resumeDocument) { this.resumeDocument = resumeDocument; }

    public String getNormalizedText() { return normalizedText; }
    public void setNormalizedText(String normalizedText) { this.normalizedText = normalizedText; }

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

    public List<ExtractedEvidence> getEvidenceItems() { return evidenceItems; }
    public void setEvidenceItems(List<ExtractedEvidence> evidenceItems) { this.evidenceItems = evidenceItems; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
