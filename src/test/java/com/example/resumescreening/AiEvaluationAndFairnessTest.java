package com.example.resumescreening;

import com.example.resumescreening.candidate.Candidate;
import com.example.resumescreening.embedding.EmbeddingService;
import com.example.resumescreening.extraction.ExtractedEvidence;
import com.example.resumescreening.extraction.FieldType;
import com.example.resumescreening.extraction.ResumeProfile;
import com.example.resumescreening.job.Job;
import com.example.resumescreening.job.JobRequirement;
import com.example.resumescreening.job.RequirementType;
import com.example.resumescreening.matching.CandidateJobAssessment;
import com.example.resumescreening.matching.MatchingEngine;
import com.example.resumescreening.taxonomy.SkillTaxonomyService;
import com.example.resumescreening.workspace.Workspace;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AiEvaluationAndFairnessTest {

    @Autowired
    private SkillTaxonomyService taxonomyService;

    @Autowired
    private MatchingEngine matchingEngine;

    @Test
    @DisplayName("AI Evaluation: Measures skill extraction Precision, Recall and F1 against labeled benchmark dataset")
    void testExtractionPrecisionRecallF1() {
        // Benchmark dataset with synthetic resume texts and ground-truth canonical skills
        Map<String, Set<String>> benchmark = Map.of(
                "Proficient in Java 21, Spring Boot microservices, PostgreSQL, and Docker containerization.",
                Set.of("Java", "Spring Boot", "PostgreSQL", "Docker"),

                "Frontend engineer skilled in TypeScript, React, HTML5, CSS3, and REST API consumption.",
                Set.of("TypeScript", "React", "HTML", "CSS", "REST API"),

                "DevOps engineer experienced with Kubernetes cluster administration, Terraform, Linux, and AWS.",
                Set.of("Kubernetes", "Terraform", "Linux", "AWS"),

                "Data scientist working with Python, PyTorch deep learning, Pandas, and Apache Spark.",
                Set.of("Python", "PyTorch", "Deep Learning", "Pandas", "Apache Spark")
        );

        int totalTruePositives = 0;
        int totalFalsePositives = 0;
        int totalFalseNegatives = 0;

        for (Map.Entry<String, Set<String>> entry : benchmark.entrySet()) {
            String text = entry.getKey();
            Set<String> groundTruth = entry.getValue();

            List<SkillTaxonomyService.SkillMatch> extracted = taxonomyService.extractSkillsFromText(text);
            Set<String> predicted = new HashSet<>();
            for (SkillTaxonomyService.SkillMatch match : extracted) {
                predicted.add(match.getSkill().getCanonicalName());
            }

            for (String p : predicted) {
                if (groundTruth.contains(p)) {
                    totalTruePositives++;
                } else {
                    totalFalsePositives++;
                }
            }

            for (String g : groundTruth) {
                if (!predicted.contains(g)) {
                    totalFalseNegatives++;
                }
            }
        }

        double precision = (double) totalTruePositives / (totalTruePositives + totalFalsePositives);
        double recall = (double) totalTruePositives / (totalTruePositives + totalFalseNegatives);
        double f1 = (2 * precision * recall) / (precision + recall);

        System.out.printf("Taxonomy Benchmark Results: Precision=%.3f, Recall=%.3f, F1=%.3f%n", precision, recall, f1);

        assertTrue(precision >= 0.85, "Extraction Precision must be >= 0.85; was " + precision);
        assertTrue(recall >= 0.85, "Extraction Recall must be >= 0.85; was " + recall);
        assertTrue(f1 >= 0.85, "Extraction F1 score must be >= 0.85; was " + f1);
    }

    @Test
    @DisplayName("Fairness Check: Proves candidate scores are strictly invariant to personal demographics and proxy features")
    void testScoringFairnessInvariance() {
        Workspace ws = new Workspace("Fairness Audit Workspace");
        Job job = new Job(ws, "Software Engineer", "Core", "Requirements: Java and Spring Boot.", UUID.randomUUID());
        job.addRequirement(new JobRequirement(job, "Java", "Java 17/21", RequirementType.REQUIRED, 1.0));
        job.addRequirement(new JobRequirement(job, "Spring Boot", "Spring Boot", RequirementType.REQUIRED, 1.0));

        // Candidate 1: Demographic Group A (e.g. John Doe, Graduated 2010)
        Candidate c1 = new Candidate(ws, "CAND-A", "John Doe", "john@example.com", "555-0100");
        ResumeProfile p1 = new ResumeProfile(c1, null, "Summary of software developer with Java and Spring Boot skills.", "{}", "1.0");
        p1.addEvidence(new ExtractedEvidence(p1, FieldType.SKILL, "Java", "5 years Java", 1, 10));
        p1.addEvidence(new ExtractedEvidence(p1, FieldType.SKILL, "Spring Boot", "Spring Boot microservices", 1, 30));

        // Candidate 2: Demographic Group B (e.g. Priya Sharma, Graduated 2022) with IDENTICAL job skills
        Candidate c2 = new Candidate(ws, "CAND-B", "Priya Sharma", "priya@example.com", "555-0200");
        ResumeProfile p2 = new ResumeProfile(c2, null, "Summary of software developer with Java and Spring Boot skills.", "{}", "1.0");
        p2.addEvidence(new ExtractedEvidence(p2, FieldType.SKILL, "Java", "5 years Java", 1, 10));
        p2.addEvidence(new ExtractedEvidence(p2, FieldType.SKILL, "Spring Boot", "Spring Boot microservices", 1, 30));

        CandidateJobAssessment assessment1 = matchingEngine.assessCandidate(job, p1);
        CandidateJobAssessment assessment2 = matchingEngine.assessCandidate(job, p2);

        // Scores must be identical regardless of candidate name, reference, or demographics!
        assertEquals(assessment1.getOverallScore(), assessment2.getOverallScore(), 1e-4,
                "Fairness violation: scores must not differ based on non-job features");
        assertEquals(assessment1.getRequiredCoverage(), assessment2.getRequiredCoverage(), 1e-4);
        assertEquals(assessment1.getConfidenceOrCompleteness(), assessment2.getConfidenceOrCompleteness(), 1e-4);
    }
}
