package com.example.resumescreening.reporting;

import java.util.Map;
import java.util.UUID;

public class JobSummaryReportDto {
    private UUID jobId;
    private String jobTitle;
    private long totalResumesProcessed;
    private double averageScore;
    private Map<String, Long> scoreDistribution;
    private Map<String, Long> frequentlyOccurringSkills;
    private Map<String, Long> reviewStatusCounts;
    private String modelVersion;
    private String taxonomyVersion;

    public JobSummaryReportDto() {}

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public long getTotalResumesProcessed() { return totalResumesProcessed; }
    public void setTotalResumesProcessed(long totalResumesProcessed) { this.totalResumesProcessed = totalResumesProcessed; }

    public double getAverageScore() { return averageScore; }
    public void setAverageScore(double averageScore) { this.averageScore = averageScore; }

    public Map<String, Long> getScoreDistribution() { return scoreDistribution; }
    public void setScoreDistribution(Map<String, Long> scoreDistribution) { this.scoreDistribution = scoreDistribution; }

    public Map<String, Long> getFrequentlyOccurringSkills() { return frequentlyOccurringSkills; }
    public void setFrequentlyOccurringSkills(Map<String, Long> frequentlyOccurringSkills) { this.frequentlyOccurringSkills = frequentlyOccurringSkills; }

    public Map<String, Long> getReviewStatusCounts() { return reviewStatusCounts; }
    public void setReviewStatusCounts(Map<String, Long> reviewStatusCounts) { this.reviewStatusCounts = reviewStatusCounts; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public String getTaxonomyVersion() { return taxonomyVersion; }
    public void setTaxonomyVersion(String taxonomyVersion) { this.taxonomyVersion = taxonomyVersion; }
}
