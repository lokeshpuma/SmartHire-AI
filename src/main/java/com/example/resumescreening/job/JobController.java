package com.example.resumescreening.job;

import com.example.resumescreening.common.ApiResponse;
import com.example.resumescreening.common.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<JobResponseDto>> createJob(@Valid @RequestBody CreateJobRequest request) {
        JobResponseDto created = jobService.createJob(request);
        return ResponseEntity.ok(ApiResponse.ok("Job created successfully", created));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<JobResponseDto>>> getPublicJobs() {
        List<JobResponseDto> openJobs = jobService.getPublicJobs();
        return ResponseEntity.ok(ApiResponse.ok(openJobs));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<JobResponseDto>>> getJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<JobResponseDto> jobPage = jobService.getJobs(pageable);

        PagedResponse<JobResponseDto> pagedResponse = new PagedResponse<>(
                jobPage.getContent(),
                jobPage.getNumber(),
                jobPage.getSize(),
                jobPage.getTotalElements(),
                jobPage.getTotalPages(),
                jobPage.isLast()
        );

        return ResponseEntity.ok(ApiResponse.ok(pagedResponse));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<JobResponseDto>> getJobById(@PathVariable UUID jobId) {
        JobResponseDto job = jobService.getJobById(jobId);
        return ResponseEntity.ok(ApiResponse.ok(job));
    }

    @PutMapping("/{jobId}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<JobResponseDto>> updateJob(
            @PathVariable UUID jobId,
            @RequestBody UpdateJobRequest request) {
        JobResponseDto updated = jobService.updateJob(jobId, request);
        return ResponseEntity.ok(ApiResponse.ok("Job updated successfully", updated));
    }

    @PostMapping("/{jobId}/requirements/parse")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<List<JobRequirementDto>>> parseRequirements(
            @PathVariable UUID jobId,
            @RequestBody(required = false) ParseRequirementsRequest request) {
        List<JobRequirementDto> parsed = jobService.parseRequirementsForJob(jobId, request);
        return ResponseEntity.ok(ApiResponse.ok("Parsed requirements successfully for review", parsed));
    }

    @PutMapping("/{jobId}/requirements")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<List<JobRequirementDto>>> updateRequirements(
            @PathVariable UUID jobId,
            @RequestBody List<JobRequirementDto> requirements) {
        List<JobRequirementDto> updated = jobService.setJobRequirements(jobId, requirements);
        return ResponseEntity.ok(ApiResponse.ok("Job requirements updated successfully", updated));
    }
}
