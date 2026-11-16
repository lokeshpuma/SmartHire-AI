package com.example.resumescreening.job;

import java.util.UUID;

public class JobRequirementDto {
    private UUID id;
    private String canonicalSkillId;
    private String requirementText;
    private RequirementType requirementType;
    private double weight = 1.0;
    private String acceptableSynonyms;
    private String evidenceGuidance;
    private boolean active = true;

    public JobRequirementDto() {}

    public JobRequirementDto(String canonicalSkillId, String requirementText, RequirementType requirementType, double weight) {
        this.canonicalSkillId = canonicalSkillId;
        this.requirementText = requirementText;
        this.requirementType = requirementType;
        this.weight = weight;
        this.active = true;
    }

    public static JobRequirementDto fromEntity(JobRequirement req) {
        JobRequirementDto dto = new JobRequirementDto();
        dto.setId(req.getId());
        dto.setCanonicalSkillId(req.getCanonicalSkillId());
        dto.setRequirementText(req.getRequirementText());
        dto.setRequirementType(req.getRequirementType());
        dto.setWeight(req.getWeight());
        dto.setAcceptableSynonyms(req.getAcceptableSynonyms());
        dto.setEvidenceGuidance(req.getEvidenceGuidance());
        dto.setActive(req.isActive());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
