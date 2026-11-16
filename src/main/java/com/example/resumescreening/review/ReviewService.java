package com.example.resumescreening.review;

import com.example.resumescreening.audit.AuditService;
import com.example.resumescreening.auth.User;
import com.example.resumescreening.auth.UserRepository;
import com.example.resumescreening.candidate.Candidate;
import com.example.resumescreening.candidate.CandidateRepository;
import com.example.resumescreening.common.ResourceNotFoundException;
import com.example.resumescreening.extraction.*;
import com.example.resumescreening.job.Job;
import com.example.resumescreening.job.JobService;
import com.example.resumescreening.security.SecurityUtils;
import com.example.resumescreening.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final CandidateReviewRepository reviewRepository;
    private final CandidateRepository candidateRepository;
    private final JobService jobService;
    private final UserRepository userRepository;
    private final ExtractedEvidenceRepository evidenceRepository;
    private final ResumeProfileRepository profileRepository;
    private final AuditService auditService;

    public ReviewService(CandidateReviewRepository reviewRepository,
                         CandidateRepository candidateRepository,
                         JobService jobService,
                         UserRepository userRepository,
                         ExtractedEvidenceRepository evidenceRepository,
                         ResumeProfileRepository profileRepository,
                         AuditService auditService) {
        this.reviewRepository = reviewRepository;
        this.candidateRepository = candidateRepository;
        this.jobService = jobService;
        this.userRepository = userRepository;
        this.evidenceRepository = evidenceRepository;
        this.profileRepository = profileRepository;
        this.auditService = auditService;
    }

    @Transactional
    public CandidateReviewDto recordReview(UUID candidateId, CreateReviewRequest request) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        Candidate candidate = candidateRepository.findByIdAndWorkspaceId(candidateId, currentUser.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));

        Job job = jobService.getJobEntity(request.getJobId());
        User reviewer = userRepository.findById(currentUser.getId()).orElse(null);

        Optional<CandidateReview> existing = reviewRepository.findByCandidateIdAndJobId(candidateId, job.getId());
        CandidateReview review;

        if (existing.isPresent()) {
            review = existing.get();
            review.setReviewer(reviewer);
            review.setReviewStatus(request.getReviewStatus() != null ? request.getReviewStatus() : ReviewStatus.REVIEWED);
            review.setDecision(request.getDecision() != null ? request.getDecision() : HumanDecision.PENDING_REVIEW);
            review.setReason(request.getReason());
            review.setNotes(request.getNotes());
            review.setRequestedReviewer(request.getRequestedReviewer());
            review.setUpdatedAt(Instant.now());
        } else {
            review = new CandidateReview(
                    candidate,
                    job,
                    reviewer,
                    request.getReviewStatus() != null ? request.getReviewStatus() : ReviewStatus.REVIEWED,
                    request.getDecision() != null ? request.getDecision() : HumanDecision.PENDING_REVIEW,
                    request.getReason(),
                    request.getNotes()
            );
            review.setRequestedReviewer(request.getRequestedReviewer());
        }

        CandidateReview saved = reviewRepository.save(review);

        if (request.getDecision() != null) {
            switch (request.getDecision()) {
                case ADVANCE_TO_INTERVIEW -> candidate.setCurrentStage("RECRUITER_SCREEN");
                case ADVANCE_TO_TECHNICAL_ROUND -> candidate.setCurrentStage("TECHNICAL_ROUND");
                case ADVANCE_TO_FINAL_ROUND -> candidate.setCurrentStage("FINAL_ROUND");
                case EXTEND_OFFER -> candidate.setCurrentStage("OFFER_EXTENDED");
                case NOT_A_FIT_FOR_ROLE -> candidate.setCurrentStage("REJECTED");
                case ON_HOLD -> candidate.setCurrentStage("ON_HOLD");
                default -> {}
            }
            candidateRepository.save(candidate);
        }

        // Update profile review status
        profileRepository.findByCandidateId(candidateId).ifPresent(p -> {
            if (saved.getReviewStatus() == ReviewStatus.REVIEWED) {
                p.setReviewStatus(ProfileReviewStatus.REVIEWED);
            } else if (saved.getReviewStatus() == ReviewStatus.NEEDS_CLARIFICATION) {
                p.setReviewStatus(ProfileReviewStatus.NEEDS_CLARIFICATION);
            } else if (saved.getReviewStatus() == ReviewStatus.CLOSED) {
                p.setReviewStatus(ProfileReviewStatus.CLOSED);
            }
            profileRepository.save(p);
        });

        auditService.logCurrentAction("RECORD_HUMAN_REVIEW", "CandidateReview", saved.getId().toString(),
                "{\"candidateRef\":\"" + candidate.getCandidateReference() + "\",\"decision\":\"" + saved.getDecision() + "\",\"reason\":\"" + saved.getReason() + "\"}");

        return CandidateReviewDto.fromEntity(saved);
    }

    @Transactional
    public ExtractedEvidenceDto updateEvidence(UUID candidateId, UUID evidenceId, UpdateEvidenceRequest request) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        Candidate candidate = candidateRepository.findByIdAndWorkspaceId(candidateId, currentUser.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));

        ResumeProfile profile = profileRepository.findByCandidateId(candidate.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for candidate: " + candidateId));

        ExtractedEvidence evidence = evidenceRepository.findByIdAndResumeProfileId(evidenceId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found: " + evidenceId));

        String priorValue = evidence.getCanonicalValue();

        if (request.getCanonicalValue() != null && !request.getCanonicalValue().isBlank()) {
            evidence.setCanonicalValue(request.getCanonicalValue().trim());
        }
        if (request.getOriginalText() != null) {
            evidence.setOriginalText(request.getOriginalText());
        }
        if (request.getReviewStatus() != null) {
            evidence.setReviewStatus(request.getReviewStatus());
        }
        if (request.getNotes() != null) {
            evidence.setNotes(request.getNotes());
        }
        evidence.setExtractionMethod(ExtractionMethod.MANUALLY_CORRECTED);

        ExtractedEvidence saved = evidenceRepository.save(evidence);

        auditService.logCurrentAction("CORRECT_EVIDENCE", "ExtractedEvidence", saved.getId().toString(),
                "{\"prior\":\"" + priorValue + "\",\"updated\":\"" + saved.getCanonicalValue() + "\",\"status\":\"" + saved.getReviewStatus() + "\"}");

        return ExtractedEvidenceDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<CandidateReviewDto> getReviewsForCandidate(UUID candidateId) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        candidateRepository.findByIdAndWorkspaceId(candidateId, currentUser.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));

        return reviewRepository.findByCandidateId(candidateId).stream()
                .map(CandidateReviewDto::fromEntity)
                .collect(Collectors.toList());
    }
}
