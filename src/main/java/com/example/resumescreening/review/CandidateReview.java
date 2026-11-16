package com.example.resumescreening.review;

import com.example.resumescreening.auth.User;
import com.example.resumescreening.candidate.Candidate;
import com.example.resumescreening.job.Job;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "candidate_reviews")
public class CandidateReview {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false)
    private ReviewStatus reviewStatus;

    @Enumerated(EnumType.STRING)
    private HumanDecision decision;

    private String reason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "requested_reviewer")
    private String requestedReviewer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CandidateReview() {
        this.id = UUID.randomUUID();
        this.reviewStatus = ReviewStatus.UNDER_REVIEW;
        this.decision = HumanDecision.PENDING_REVIEW;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public CandidateReview(Candidate candidate, Job job, User reviewer, ReviewStatus reviewStatus, HumanDecision decision, String reason, String notes) {
        this();
        this.candidate = candidate;
        this.job = job;
        this.reviewer = reviewer;
        this.reviewStatus = reviewStatus;
        this.decision = decision;
        this.reason = reason;
        this.notes = notes;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Candidate getCandidate() { return candidate; }
    public void setCandidate(Candidate candidate) { this.candidate = candidate; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public User getReviewer() { return reviewer; }
    public void setReviewer(User reviewer) { this.reviewer = reviewer; }

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
