package com.example.resumescreening.matching;

import java.time.Instant;
import java.util.UUID;

public class AssessmentResponseDto {
    private UUID id;
    private UUID candidateId;
    private String candidateReference;
    private String candidateDisplayName;
    private UUID jobId;
    private String jobTitle;
    private double overallScore;
    private double confidenceOrCompleteness;
    private double requiredCoverage;
    private double preferredCoverage;
    private double semanticRelevance;
    private double evidenceCoverage;
    private String scoreBreakdownJson;
    private String matchedEvidenceJson;
    private String missingOrUnknownRequirementsJson;
    private String embeddingModelVersion;
    private String scoringConfigVersion;
    private Instant createdAt;

    public AssessmentResponseDto() {}

    public static AssessmentResponseDto fromEntity(CandidateJobAssessment assessment) {
        AssessmentResponseDto dto = new AssessmentResponseDto();
        dto.setId(assessment.getId());
        dto.setCandidateId(assessment.getCandidate().getId());
        dto.setCandidateReference(assessment.getCandidate().getCandidateReference());
        dto.setCandidateDisplayName(assessment.getCandidate().getDisplayName());
        dto.setJobId(assessment.getJob().getId());
        dto.setJobTitle(assessment.getJob().getTitle());
        dto.setOverallScore(assessment.getOverallScore());
        dto.setConfidenceOrCompleteness(assessment.getConfidenceOrCompleteness());
        dto.setRequiredCoverage(assessment.getRequiredCoverage());
        dto.setPreferredCoverage(assessment.getPreferredCoverage());
        dto.setSemanticRelevance(assessment.getSemanticRelevance());
        dto.setEvidenceCoverage(assessment.getEvidenceCoverage());
        dto.setScoreBreakdownJson(assessment.getScoreBreakdownJson());
        dto.setMatchedEvidenceJson(assessment.getMatchedEvidenceJson());
        dto.setMissingOrUnknownRequirementsJson(assessment.getMissingOrUnknownRequirementsJson());
        dto.setEmbeddingModelVersion(assessment.getEmbeddingModelVersion());
        dto.setScoringConfigVersion(assessment.getScoringConfigVersion());
        dto.setCreatedAt(assessment.getCreatedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCandidateId() { return candidateId; }
    public void setCandidateId(UUID candidateId) { this.candidateId = candidateId; }

    public String getCandidateReference() { return candidateReference; }
    public void setCandidateReference(String candidateReference) { this.candidateReference = candidateReference; }

    public String getCandidateDisplayName() { return candidateDisplayName; }
    public void setCandidateDisplayName(String candidateDisplayName) { this.candidateDisplayName = candidateDisplayName; }

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public double getOverallScore() { return overallScore; }
    public void setOverallScore(double overallScore) { this.overallScore = overallScore; }

    public double getConfidenceOrCompleteness() { return confidenceOrCompleteness; }
    public void setConfidenceOrCompleteness(double confidenceOrCompleteness) { this.confidenceOrCompleteness = confidenceOrCompleteness; }

    public double getRequiredCoverage() { return requiredCoverage; }
    public void setRequiredCoverage(double requiredCoverage) { this.requiredCoverage = requiredCoverage; }

    public double getPreferredCoverage() { return preferredCoverage; }
    public void setPreferredCoverage(double preferredCoverage) { this.preferredCoverage = preferredCoverage; }

    public double getSemanticRelevance() { return semanticRelevance; }
    public void setSemanticRelevance(double semanticRelevance) { this.semanticRelevance = semanticRelevance; }

    public double getEvidenceCoverage() { return evidenceCoverage; }
    public void setEvidenceCoverage(double evidenceCoverage) { this.evidenceCoverage = evidenceCoverage; }

    public String getScoreBreakdownJson() { return scoreBreakdownJson; }
    public void setScoreBreakdownJson(String scoreBreakdownJson) { this.scoreBreakdownJson = scoreBreakdownJson; }

    public String getMatchedEvidenceJson() { return matchedEvidenceJson; }
    public void setMatchedEvidenceJson(String matchedEvidenceJson) { this.matchedEvidenceJson = matchedEvidenceJson; }

    public String getMissingOrUnknownRequirementsJson() { return missingOrUnknownRequirementsJson; }
    public void setMissingOrUnknownRequirementsJson(String missingOrUnknownRequirementsJson) { this.missingOrUnknownRequirementsJson = missingOrUnknownRequirementsJson; }

    public String getEmbeddingModelVersion() { return embeddingModelVersion; }
    public void setEmbeddingModelVersion(String embeddingModelVersion) { this.embeddingModelVersion = embeddingModelVersion; }

    public String getScoringConfigVersion() { return scoringConfigVersion; }
    public void setScoringConfigVersion(String scoringConfigVersion) { this.scoringConfigVersion = scoringConfigVersion; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
