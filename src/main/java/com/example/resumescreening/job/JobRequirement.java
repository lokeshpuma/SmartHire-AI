package com.example.resumescreening.job;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "job_requirements")
public class JobRequirement {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "canonical_skill_id")
    private String canonicalSkillId;

    @Column(name = "requirement_text", columnDefinition = "TEXT", nullable = false)
    private String requirementText;

    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_type", nullable = false)
    private RequirementType requirementType;

    @Column(nullable = false)
    private double weight = 1.0;

    @Column(name = "acceptable_synonyms", columnDefinition = "TEXT")
    private String acceptableSynonyms;

    @Column(name = "evidence_guidance", columnDefinition = "TEXT")
    private String evidenceGuidance;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public JobRequirement() {
        this.id = UUID.randomUUID();
        this.requirementType = RequirementType.REQUIRED;
        this.weight = 1.0;
        this.isActive = true;
    }

    public JobRequirement(Job job, String canonicalSkillId, String requirementText, RequirementType requirementType, double weight) {
        this();
        this.job = job;
        this.canonicalSkillId = canonicalSkillId;
        this.requirementText = requirementText;
        this.requirementType = requirementType;
        this.weight = weight;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public String getCanonicalSkillId() { return canonicalSkillId; }
    public void setCanonicalSkillId(String canonicalSkillId) { this.canonicalSkillId = canonicalSkillId; }

    public String getRequirementText() { return requirementText; }
    public void setRequirementText(String requirementText) { this.requirementText = requirementText; }

    public RequirementType getRequirementType() { return requirementType; }
    public void setRequirementType(RequirementType requirementType) { this.requirementType = requirementType; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public String getAcceptableSynonyms() { return acceptableSynonyms; }
    public void setAcceptableSynonyms(String acceptableSynonyms) { this.acceptableSynonyms = acceptableSynonyms; }

    public String getEvidenceGuidance() { return evidenceGuidance; }
    public void setEvidenceGuidance(String evidenceGuidance) { this.evidenceGuidance = evidenceGuidance; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
