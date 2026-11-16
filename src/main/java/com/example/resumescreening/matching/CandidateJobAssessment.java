package com.example.resumescreening.matching;

import com.example.resumescreening.candidate.Candidate;
import com.example.resumescreening.job.Job;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "candidate_job_assessments")
public class CandidateJobAssessment {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "overall_score", nullable = false)
    private double overallScore;

    @Column(name = "confidence_or_completeness", nullable = false)
    private double confidenceOrCompleteness;

    @Column(name = "required_coverage", nullable = false)
    private double requiredCoverage;

    @Column(name = "preferred_coverage", nullable = false)
    private double preferredCoverage;

    @Column(name = "semantic_relevance", nullable = false)
    private double semanticRelevance;

    @Column(name = "evidence_coverage", nullable = false)
    private double evidenceCoverage;

    @Column(name = "score_breakdown_json", columnDefinition = "TEXT")
    private String scoreBreakdownJson;

    @Column(name = "matched_evidence_json", columnDefinition = "TEXT")
    private String matchedEvidenceJson;

    @Column(name = "missing_or_unknown_requirements_json", columnDefinition = "TEXT")
    private String missingOrUnknownRequirementsJson;

    @Column(name = "embedding_model_version")
    private String embeddingModelVersion;

    @Column(name = "scoring_config_version")
    private String scoringConfigVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CandidateJobAssessment() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Candidate getCandidate() { return candidate; }
    public void setCandidate(Candidate candidate) { this.candidate = candidate; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

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
