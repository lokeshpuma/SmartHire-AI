package com.example.resumescreening.reporting;

import com.example.resumescreening.audit.AuditService;
import com.example.resumescreening.embedding.EmbeddingService;
import com.example.resumescreening.extraction.ExtractedEvidence;
import com.example.resumescreening.extraction.FieldType;
import com.example.resumescreening.extraction.ResumeProfile;
import com.example.resumescreening.extraction.ResumeProfileRepository;
import com.example.resumescreening.job.Job;
import com.example.resumescreening.job.JobService;
import com.example.resumescreening.matching.CandidateJobAssessment;
import com.example.resumescreening.matching.CandidateJobAssessmentRepository;
import com.example.resumescreening.review.CandidateReview;
import com.example.resumescreening.review.CandidateReviewRepository;
import com.example.resumescreening.taxonomy.SkillTaxonomyService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

@Service
public class ReportingService {

    private final JobService jobService;
    private final CandidateJobAssessmentRepository assessmentRepository;
    private final ResumeProfileRepository profileRepository;
    private final CandidateReviewRepository reviewRepository;
    private final SkillTaxonomyService taxonomyService;
    private final EmbeddingService embeddingService;
    private final AuditService auditService;

    public ReportingService(JobService jobService,
                            CandidateJobAssessmentRepository assessmentRepository,
                            ResumeProfileRepository profileRepository,
                            CandidateReviewRepository reviewRepository,
                            SkillTaxonomyService taxonomyService,
                            EmbeddingService embeddingService,
                            AuditService auditService) {
        this.jobService = jobService;
        this.assessmentRepository = assessmentRepository;
        this.profileRepository = profileRepository;
        this.reviewRepository = reviewRepository;
        this.taxonomyService = taxonomyService;
        this.embeddingService = embeddingService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public JobSummaryReportDto generateJobSummaryReport(UUID jobId) {
        Job job = jobService.getJobEntity(jobId);
        List<CandidateJobAssessment> assessments = assessmentRepository.findByJobIdOrderByOverallScoreDesc(jobId);

        JobSummaryReportDto report = new JobSummaryReportDto();
        report.setJobId(jobId);
        report.setJobTitle(job.getTitle());
        report.setTotalResumesProcessed(assessments.size());
        report.setModelVersion(embeddingService.getModelName() + ":" + embeddingService.getModelVersion());
        report.setTaxonomyVersion(taxonomyService.getCurrentTaxonomyVersion());

        double totalScore = 0.0;
        Map<String, Long> scoreBuckets = new LinkedHashMap<>();
        scoreBuckets.put("0-25%", 0L);
        scoreBuckets.put("25-50%", 0L);
        scoreBuckets.put("50-75%", 0L);
        scoreBuckets.put("75-100%", 0L);

        Map<String, Long> skillCounts = new HashMap<>();

        for (CandidateJobAssessment ass : assessments) {
            double s = ass.getOverallScore();
            totalScore += s;

            if (s < 0.25) scoreBuckets.put("0-25%", scoreBuckets.get("0-25%") + 1);
            else if (s < 0.50) scoreBuckets.put("25-50%", scoreBuckets.get("25-50%") + 1);
            else if (s < 0.75) scoreBuckets.put("50-75%", scoreBuckets.get("50-75%") + 1);
            else scoreBuckets.put("75-100%", scoreBuckets.get("75-100%") + 1);

            // Skills frequency in applicant pool
            profileRepository.findByCandidateIdWithEvidence(ass.getCandidate().getId()).ifPresent(p -> {
                if (p.getEvidenceItems() != null) {
                    for (ExtractedEvidence ev : p.getEvidenceItems()) {
                        if (ev.getFieldType() == FieldType.SKILL || ev.getFieldType() == FieldType.TOOL) {
                            skillCounts.put(ev.getCanonicalValue(), skillCounts.getOrDefault(ev.getCanonicalValue(), 0L) + 1);
                        }
                    }
                }
            });
        }

        report.setAverageScore(assessments.isEmpty() ? 0.0 : Math.round((totalScore / assessments.size()) * 1000.0) / 1000.0);
        report.setScoreDistribution(scoreBuckets);

        // Sort frequently occurring skills top 15
        Map<String, Long> topSkills = new LinkedHashMap<>();
        skillCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(15)
                .forEach(e -> topSkills.put(e.getKey(), e.getValue()));
        report.setFrequentlyOccurringSkills(topSkills);

        // Review status breakdown
        List<CandidateReview> reviews = reviewRepository.findByJobId(jobId);
        Map<String, Long> statusCounts = new HashMap<>();
        for (CandidateReview r : reviews) {
            String status = r.getReviewStatus() != null ? r.getReviewStatus().name() : "NOT_REVIEWED";
            statusCounts.put(status, statusCounts.getOrDefault(status, 0L) + 1);
        }
        report.setReviewStatusCounts(statusCounts);

        auditService.logCurrentAction("VIEW_JOB_REPORT", "JobReport", jobId.toString(),
                "{\"assessedCandidates\":" + assessments.size() + "}");

        return report;
    }

    @Transactional(readOnly = true)
    public void exportCandidatesCsv(UUID jobId, Writer writer) throws IOException {
        Job job = jobService.getJobEntity(jobId);
        List<CandidateJobAssessment> assessments = assessmentRepository.findByJobIdOrderByOverallScoreDesc(jobId);

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(
                        "Candidate Reference",
                        "Match Score",
                        "Confidence / Completeness",
                        "Required Skill Coverage",
                        "Preferred Skill Coverage",
                        "Semantic Relevance",
                        "Evidence Quality",
                        "Review Status",
                        "Human Decision",
                        "Assessment Timestamp"
                )
                .build();

        try (CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {
            for (CandidateJobAssessment ass : assessments) {
                Optional<CandidateReview> reviewOpt = reviewRepository.findByCandidateIdAndJobId(ass.getCandidate().getId(), jobId);

                String reviewStatus = reviewOpt.map(r -> r.getReviewStatus().name()).orElse("NOT_REVIEWED");
                String decision = reviewOpt.map(r -> r.getDecision() != null ? r.getDecision().name() : "").orElse("");

                // Output only job-relevant, non-protected attributes (Section 4.11 & 6.2)
                printer.printRecord(
                        ass.getCandidate().getCandidateReference(),
                        String.format("%.3f", ass.getOverallScore()),
                        String.format("%.3f", ass.getConfidenceOrCompleteness()),
                        String.format("%.3f", ass.getRequiredCoverage()),
                        String.format("%.3f", ass.getPreferredCoverage()),
                        String.format("%.3f", ass.getSemanticRelevance()),
                        String.format("%.3f", ass.getEvidenceCoverage()),
                        reviewStatus,
                        decision,
                        ass.getCreatedAt().toString()
                );
            }
        }

        auditService.logCurrentAction("EXPORT_CANDIDATES_CSV", "JobExport", jobId.toString(),
                "{\"exportedRows\":" + assessments.size() + "}");
    }
}
