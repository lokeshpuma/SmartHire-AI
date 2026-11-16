package com.example.resumescreening.review;

import java.util.UUID;

public class CreateReviewRequest {
    private UUID jobId;
    private ReviewStatus reviewStatus;
    private HumanDecision decision;
    private String reason;
    private String notes;
    private String requestedReviewer;

    public CreateReviewRequest() {}

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

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
}
