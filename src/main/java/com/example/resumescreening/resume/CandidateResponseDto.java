package com.example.resumescreening.resume;

import com.example.resumescreening.candidate.Candidate;
import com.example.resumescreening.extraction.ProfileReviewStatus;
import com.example.resumescreening.extraction.ResumeProfileDto;
import com.example.resumescreening.matching.AssessmentResponseDto;

import java.time.Instant;
import java.util.UUID;

public class CandidateResponseDto {
    private UUID id;
    private UUID workspaceId;
    private String candidateReference;
    private String displayName;
    private String email;
    private String phone;
    private ResumeProfileDto profile;
    private AssessmentResponseDto assessment;
    private ProfileReviewStatus reviewStatus;
    private String decision;
    private String source;
    private String currentStage;
    private Instant createdAt;

    public CandidateResponseDto() {}

    public static CandidateResponseDto fromEntity(Candidate candidate) {
        CandidateResponseDto dto = new CandidateResponseDto();
        dto.setId(candidate.getId());
        dto.setWorkspaceId(candidate.getWorkspace().getId());
        dto.setCandidateReference(candidate.getCandidateReference());
        dto.setDisplayName(candidate.getDisplayName());
        dto.setEmail(candidate.getEmail());
        dto.setPhone(candidate.getPhone());
        dto.setSource(candidate.getSource() != null ? candidate.getSource() : "DIRECT_UPLOAD");
        dto.setCurrentStage(candidate.getCurrentStage() != null ? candidate.getCurrentStage() : "SCREENING");
        dto.setCreatedAt(candidate.getCreatedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }

    public String getCandidateReference() { return candidateReference; }
    public void setCandidateReference(String candidateReference) { this.candidateReference = candidateReference; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public ResumeProfileDto getProfile() { return profile; }
    public void setProfile(ResumeProfileDto profile) { this.profile = profile; }

    public AssessmentResponseDto getAssessment() { return assessment; }
    public void setAssessment(AssessmentResponseDto assessment) { this.assessment = assessment; }

    public ProfileReviewStatus getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(ProfileReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
