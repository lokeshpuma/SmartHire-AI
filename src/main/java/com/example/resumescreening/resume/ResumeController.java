package com.example.resumescreening.resume;

import com.example.resumescreening.common.ApiResponse;
import com.example.resumescreening.common.PagedResponse;
import com.example.resumescreening.extraction.ExtractedEvidenceDto;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ResumeController {

    private final ResumeProcessingService resumeService;

    public ResumeController(ResumeProcessingService resumeService) {
        this.resumeService = resumeService;
    }

    @GetMapping("/public/jobs")
    public ResponseEntity<ApiResponse<List<com.example.resumescreening.job.JobResponseDto>>> getPublicJobs() {
        return ResponseEntity.ok(ApiResponse.ok(resumeService.getJobService().getPublicJobs()));
    }

    @PostMapping(value = "/public/jobs/{jobId}/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ResumeUploadResponse>> applyAsCandidate(
            @PathVariable UUID jobId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("fullName") String fullName,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "notes", required = false) String notes) {
        ResumeUploadResponse response = resumeService.processCandidateApplication(jobId, file, fullName, email, phone, notes);
        return ResponseEntity.ok(ApiResponse.ok("Candidate application submitted successfully", response));
    }

    @GetMapping("/public/candidates/track")
    public ResponseEntity<ApiResponse<CandidateResponseDto>> trackCandidateApplication(
            @RequestParam("reference") String reference) {
        CandidateResponseDto dto = resumeService.trackCandidateApplication(reference);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @PostMapping(value = "/jobs/{jobId}/resumes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<ResumeUploadResponse>> uploadResume(
            @PathVariable UUID jobId,
            @RequestParam("file") MultipartFile file) {
        ResumeUploadResponse response = resumeService.processSingleResume(jobId, file);
        return ResponseEntity.ok(ApiResponse.ok("Resume upload processed", response));
    }

    @PostMapping(value = "/jobs/{jobId}/resumes/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<BatchUploadResponse>> uploadBatchResumes(
            @PathVariable UUID jobId,
            @RequestParam("files") List<MultipartFile> files) {
        BatchUploadResponse response = resumeService.processBatchResumes(jobId, files);
        return ResponseEntity.ok(ApiResponse.ok("Batch resume upload completed", response));
    }

    @GetMapping("/jobs/{jobId}/candidates")
    public ResponseEntity<ApiResponse<PagedResponse<CandidateResponseDto>>> getCandidatesForJob(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CandidateResponseDto> candidatePage = resumeService.getCandidatesForJob(jobId, pageable);

        PagedResponse<CandidateResponseDto> response = new PagedResponse<>(
                candidatePage.getContent(),
                candidatePage.getNumber(),
                candidatePage.getSize(),
                candidatePage.getTotalElements(),
                candidatePage.getTotalPages(),
                candidatePage.isLast()
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/candidates/{candidateId}")
    public ResponseEntity<ApiResponse<CandidateResponseDto>> getCandidateById(@PathVariable UUID candidateId) {
        CandidateResponseDto candidate = resumeService.getCandidateById(candidateId);
        return ResponseEntity.ok(ApiResponse.ok(candidate));
    }

    @GetMapping("/candidates/{candidateId}/evidence")
    public ResponseEntity<ApiResponse<List<ExtractedEvidenceDto>>> getCandidateEvidence(@PathVariable UUID candidateId) {
        List<ExtractedEvidenceDto> evidence = resumeService.getCandidateEvidence(candidateId);
        return ResponseEntity.ok(ApiResponse.ok(evidence));
    }

    @DeleteMapping("/candidates/{candidateId}/resume")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<Void>> deleteCandidateResume(@PathVariable UUID candidateId) {
        resumeService.deleteCandidateResume(candidateId);
        return ResponseEntity.ok(ApiResponse.ok("Candidate resume and derived evidence deleted according to retention policy", null));
    }

    @GetMapping("/candidates/{candidateId}/resume/download")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMINISTRATOR', 'HIRING_MANAGER')")
    public ResponseEntity<Resource> downloadResume(@PathVariable UUID candidateId) throws IOException {
        Resource resource = resumeService.loadResumeResource(candidateId);
        String filename = resumeService.getResumeOriginalFilename(candidateId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }
}
