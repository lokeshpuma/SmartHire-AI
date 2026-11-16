package com.example.resumescreening.reporting;

import com.example.resumescreening.common.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs/{jobId}")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/reports/summary")
    public ResponseEntity<ApiResponse<JobSummaryReportDto>> getJobSummary(@PathVariable UUID jobId) {
        JobSummaryReportDto summary = reportingService.generateJobSummaryReport(jobId);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/exports/candidates.csv")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMINISTRATOR')")
    public void exportCandidatesCsv(@PathVariable UUID jobId, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"candidates-job-" + jobId + ".csv\"");
        reportingService.exportCandidatesCsv(jobId, response.getWriter());
    }
}
