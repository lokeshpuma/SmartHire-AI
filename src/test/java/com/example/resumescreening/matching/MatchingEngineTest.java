package com.example.resumescreening.matching;

import com.example.resumescreening.candidate.Candidate;
import com.example.resumescreening.embedding.EmbeddingService;
import com.example.resumescreening.embedding.LocalDeterministicEmbeddingService;
import com.example.resumescreening.extraction.ExtractedEvidence;
import com.example.resumescreening.extraction.FieldType;
import com.example.resumescreening.extraction.ResumeProfile;
import com.example.resumescreening.job.Job;
import com.example.resumescreening.job.JobRequirement;
import com.example.resumescreening.job.RequirementType;
import com.example.resumescreening.taxonomy.SkillTaxonomyService;
import com.example.resumescreening.workspace.Workspace;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MatchingEngineTest {

    private MatchingEngine matchingEngine;
    private EmbeddingService embeddingService;

    @Mock
    private SkillTaxonomyService taxonomyService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        embeddingService = new LocalDeterministicEmbeddingService("dense-subword-test", 128);
        objectMapper = new ObjectMapper();

        // 45% required, 20% preferred, 20% semantic, 15% evidence
        matchingEngine = new MatchingEngine(
                embeddingService,
                taxonomyService,
                objectMapper,
                0.45,
                0.20,
                0.20,
                0.15
        );
    }

    @Test
    @DisplayName("Produces high score when candidate matches all required and preferred criteria with evidence")
    void testPerfectMatchCandidate() {
        Workspace ws = new Workspace("Test Workspace");
        Job job = new Job(ws, "Backend Lead", "Engineering", "Senior Java Engineer with Spring Boot and PostgreSQL.", UUID.randomUUID());
        job.addRequirement(new JobRequirement(job, "Java", "Core Java proficiency", RequirementType.REQUIRED, 1.0));
        job.addRequirement(new JobRequirement(job, "Spring Boot", "Microservices with Spring Boot", RequirementType.REQUIRED, 1.0));
        job.addRequirement(new JobRequirement(job, "PostgreSQL", "Relational database", RequirementType.PREFERRED, 0.5));

        Candidate candidate = new Candidate(ws, "CAND-001", "Alex Developer", "alex@test.com", null);
        ResumeProfile profile = new ResumeProfile(candidate, null, "Experienced Senior Backend Developer with 6 years in Java, Spring Boot, and PostgreSQL databases.", "{}", "1.0");

        profile.addEvidence(new ExtractedEvidence(profile, FieldType.SKILL, "Java", "6 years of Java engineering", 1, 10));
        profile.addEvidence(new ExtractedEvidence(profile, FieldType.SKILL, "Spring Boot", "Designed microservices in Spring Boot", 1, 40));
        profile.addEvidence(new ExtractedEvidence(profile, FieldType.SKILL, "PostgreSQL", "Optimized queries in PostgreSQL", 1, 80));

        CandidateJobAssessment assessment = matchingEngine.assessCandidate(job, profile);

        assertNotNull(assessment);
        assertTrue(assessment.getOverallScore() > 0.80, "Expected score > 0.80 but was " + assessment.getOverallScore());
        assertEquals(1.0, assessment.getRequiredCoverage());
        assertEquals(1.0, assessment.getPreferredCoverage());
        assertTrue(assessment.getConfidenceOrCompleteness() > 0.50);
    }

    @Test
    @DisplayName("Handles missing criteria as unknown without negative score penalty")
    void testMissingCriteriaTreatedAsUnknown() {
        Workspace ws = new Workspace("Test Workspace");
        Job job = new Job(ws, "Backend Lead", "Engineering", "Java Engineer needed.", UUID.randomUUID());
        job.addRequirement(new JobRequirement(job, "Java", "Core Java", RequirementType.REQUIRED, 1.0));
        job.addRequirement(new JobRequirement(job, "Kubernetes", "Container orchestration", RequirementType.REQUIRED, 1.0));

        Candidate candidate = new Candidate(ws, "CAND-002", "Jordan Coder", "jordan@test.com", null);
        ResumeProfile profile = new ResumeProfile(candidate, null, "Software engineer specializing in Java development.", "{}", "1.0");

        profile.addEvidence(new ExtractedEvidence(profile, FieldType.SKILL, "Java", "Strong Java background", 1, 10));

        CandidateJobAssessment assessment = matchingEngine.assessCandidate(job, profile);

        // Required coverage is 50% (1 of 2 required matched)
        assertEquals(0.50, assessment.getRequiredCoverage());
        assertTrue(assessment.getOverallScore() > 0.20 && assessment.getOverallScore() < 0.80);
        // Missing requirements are recorded in JSON for recruiter inspection
        assertTrue(assessment.getMissingOrUnknownRequirementsJson().contains("Kubernetes"));
    }

    @Test
    @DisplayName("Separately exposes score and data completeness values")
    void testScoreAndCompletenessSeparation() {
        Workspace ws = new Workspace("Test Workspace");
        Job job = new Job(ws, "DevOps Engineer", "Infra", "Kubernetes engineer.", UUID.randomUUID());
        job.addRequirement(new JobRequirement(job, "Kubernetes", "k8s clusters", RequirementType.REQUIRED, 1.0));

        Candidate candidate = new Candidate(ws, "CAND-003", "Morgan Tech", "morgan@test.com", null);
        ResumeProfile profile = new ResumeProfile(candidate, null, "Short profile.", "{}", "1.0");

        CandidateJobAssessment assessment = matchingEngine.assessCandidate(job, profile);

        assertNotNull(assessment.getOverallScore());
        assertNotNull(assessment.getConfidenceOrCompleteness());
        // Scores and completeness are distinct metrics
        assertNotEquals(Double.NaN, assessment.getOverallScore());
        assertNotEquals(Double.NaN, assessment.getConfidenceOrCompleteness());
    }
}
