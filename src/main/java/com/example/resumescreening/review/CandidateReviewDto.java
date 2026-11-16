package com.example.resumescreening.review;

import java.time.Instant;
import java.util.UUID;

public class CandidateReviewDto {
    private UUID id;
    private UUID candidateId;
    private UUID jobId;
    private UUID reviewerId;
    private String reviewerName;
    private ReviewStatus reviewStatus;
    private HumanDecision decision;
    private String reason;
    private String notes;
    private String requestedReviewer;
    private Instant createdAt;
    private Instant updatedAt;

    public CandidateReviewDto() {}

    public static CandidateReviewDto fromEntity(CandidateReview review) {
        CandidateReviewDto dto = new CandidateReviewDto();
        dto.setId(review.getId());
        dto.setCandidateId(review.getCandidate().getId());
        dto.setJobId(review.getJob().getId());
        if (review.getReviewer() != null) {
            dto.setReviewerId(review.getReviewer().getId());
            dto.setReviewerName(review.getReviewer().getName());
        }
        dto.setReviewStatus(review.getReviewStatus());
        dto.setDecision(review.getDecision());
        dto.setReason(review.getReason());
        dto.setNotes(review.getNotes());
        dto.setRequestedReviewer(review.getRequestedReviewer());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setUpdatedAt(review.getUpdatedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCandidateId() { return candidateId; }
    public void setCandidateId(UUID candidateId) { this.candidateId = candidateId; }

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public UUID getReviewerId() { return reviewerId; }
    public void setReviewerId(UUID reviewerId) { this.reviewerId = reviewerId; }

    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }

    public ReviewStatus getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(ReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }

    public HumanDecision getDecision() { return decision; }
    public void setDecision(HumanDecision decision) { this.decision = decision; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getRequestedReviewer() { return requestedReviewer; }
    public void setRequestedReviewer(String requestedReviewer) { this.requestedReviewer = requestedReviewer; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
