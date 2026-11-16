package com.example.resumescreening.matching;

import com.example.resumescreening.common.ApiResponse;
import com.example.resumescreening.common.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @PostMapping("/jobs/{jobId}/assessments")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<List<AssessmentResponseDto>>> runAssessments(
            @PathVariable UUID jobId,
            @RequestParam(required = false) UUID candidateId) {
        List<AssessmentResponseDto> assessments = matchingService.runAssessmentsForJob(jobId, candidateId);
        return ResponseEntity.ok(ApiResponse.ok("Assessments generated successfully", assessments));
    }

    @GetMapping("/jobs/{jobId}/assessments")
    public ResponseEntity<ApiResponse<PagedResponse<AssessmentResponseDto>>> getJobAssessments(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AssessmentResponseDto> assessmentPage = matchingService.getAssessmentsForJob(jobId, pageable);

        PagedResponse<AssessmentResponseDto> response = new PagedResponse<>(
                assessmentPage.getContent(),
                assessmentPage.getNumber(),
                assessmentPage.getSize(),
                assessmentPage.getTotalElements(),
                assessmentPage.getTotalPages(),
                assessmentPage.isLast()
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/candidates/{candidateId}/assessments/{jobId}")
    public ResponseEntity<ApiResponse<AssessmentResponseDto>> getCandidateAssessment(
            @PathVariable UUID candidateId,
            @PathVariable UUID jobId) {
        AssessmentResponseDto assessment = matchingService.getAssessmentForCandidateAndJob(candidateId, jobId);
        return ResponseEntity.ok(ApiResponse.ok(assessment));
    }
}
