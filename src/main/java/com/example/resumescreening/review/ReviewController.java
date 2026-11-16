package com.example.resumescreening.review;

import com.example.resumescreening.common.ApiResponse;
import com.example.resumescreening.extraction.ExtractedEvidenceDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/candidates/{candidateId}")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/reviews")
    @PreAuthorize("hasAnyRole('RECRUITER', 'HIRING_MANAGER', 'ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<CandidateReviewDto>> recordReview(
            @PathVariable UUID candidateId,
            @RequestBody CreateReviewRequest request) {
        CandidateReviewDto review = reviewService.recordReview(candidateId, request);
        return ResponseEntity.ok(ApiResponse.ok("Review recorded successfully", review));
    }

    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<List<CandidateReviewDto>>> getCandidateReviews(@PathVariable UUID candidateId) {
        List<CandidateReviewDto> reviews = reviewService.getReviewsForCandidate(candidateId);
        return ResponseEntity.ok(ApiResponse.ok(reviews));
    }

    @PutMapping("/evidence/{evidenceId}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<ExtractedEvidenceDto>> updateEvidence(
            @PathVariable UUID candidateId,
            @PathVariable UUID evidenceId,
            @RequestBody UpdateEvidenceRequest request) {
        ExtractedEvidenceDto updated = reviewService.updateEvidence(candidateId, evidenceId, request);
        return ResponseEntity.ok(ApiResponse.ok("Evidence updated and audit history logged", updated));
    }
}
