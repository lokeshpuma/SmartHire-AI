package com.example.resumescreening.matching;

import com.example.resumescreening.audit.AuditService;
import com.example.resumescreening.candidate.Candidate;
import com.example.resumescreening.candidate.CandidateRepository;
import com.example.resumescreening.common.ResourceNotFoundException;
import com.example.resumescreening.extraction.ResumeProfile;
import com.example.resumescreening.extraction.ResumeProfileRepository;
import com.example.resumescreening.job.Job;
import com.example.resumescreening.job.JobService;
import com.example.resumescreening.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MatchingService {

    private final MatchingEngine matchingEngine;
    private final CandidateJobAssessmentRepository assessmentRepository;
    private final JobService jobService;
    private final ResumeProfileRepository profileRepository;
    private final CandidateRepository candidateRepository;
    private final AuditService auditService;

    public MatchingService(MatchingEngine matchingEngine,
                           CandidateJobAssessmentRepository assessmentRepository,
                           JobService jobService,
                           ResumeProfileRepository profileRepository,
                           CandidateRepository candidateRepository,
                           AuditService auditService) {
        this.matchingEngine = matchingEngine;
        this.assessmentRepository = assessmentRepository;
        this.jobService = jobService;
        this.profileRepository = profileRepository;
        this.candidateRepository = candidateRepository;
        this.auditService = auditService;
    }

    @Transactional
    public List<AssessmentResponseDto> runAssessmentsForJob(UUID jobId, UUID specificCandidateId) {
        Job job = jobService.getJobEntity(jobId);
        List<CandidateJobAssessment> assessments = new ArrayList<>();

        if (specificCandidateId != null) {
            CandidateJobAssessment assessment = assessCandidateForJob(job, specificCandidateId);
            assessments.add(assessment);
        } else {
            // Assess all candidates in the workspace with available profiles
            UUID workspaceId = SecurityUtils.getCurrentWorkspaceId();
            List<ResumeProfile> profiles = profileRepository.findAll();
            for (ResumeProfile profile : profiles) {
                if (profile.getCandidate().getWorkspace().getId().equals(workspaceId)) {
                    CandidateJobAssessment assessment = assessSingleProfile(job, profile);
                    assessments.add(assessment);
                }
            }
        }

        auditService.logCurrentAction("RUN_ASSESSMENTS", "Job", jobId.toString(),
                "{\"assessedCount\":" + assessments.size() + "}");

        return assessments.stream()
                .map(AssessmentResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public CandidateJobAssessment assessCandidateForJob(Job job, UUID candidateId) {
        UUID workspaceId = SecurityUtils.getCurrentWorkspaceId();
        Candidate candidate = candidateRepository.findByIdAndWorkspaceId(candidateId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));

        ResumeProfile profile = profileRepository.findByCandidateIdWithEvidence(candidate.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No extracted resume profile found for candidate: " + candidateId));

        return assessSingleProfile(job, profile);
    }

    private CandidateJobAssessment assessSingleProfile(Job job, ResumeProfile profile) {
        // Check if an assessment already exists
        Optional<CandidateJobAssessment> existing = assessmentRepository.findByJobIdAndCandidateId(job.getId(), profile.getCandidate().getId());

        CandidateJobAssessment assessment = matchingEngine.assessCandidate(job, profile);

        if (existing.isPresent()) {
            CandidateJobAssessment toUpdate = existing.get();
            toUpdate.setOverallScore(assessment.getOverallScore());
            toUpdate.setConfidenceOrCompleteness(assessment.getConfidenceOrCompleteness());
            toUpdate.setRequiredCoverage(assessment.getRequiredCoverage());
            toUpdate.setPreferredCoverage(assessment.getPreferredCoverage());
            toUpdate.setSemanticRelevance(assessment.getSemanticRelevance());
            toUpdate.setEvidenceCoverage(assessment.getEvidenceCoverage());
            toUpdate.setScoreBreakdownJson(assessment.getScoreBreakdownJson());
            toUpdate.setMatchedEvidenceJson(assessment.getMatchedEvidenceJson());
            toUpdate.setMissingOrUnknownRequirementsJson(assessment.getMissingOrUnknownRequirementsJson());
            toUpdate.setEmbeddingModelVersion(assessment.getEmbeddingModelVersion());
            toUpdate.setScoringConfigVersion(assessment.getScoringConfigVersion());
            return assessmentRepository.save(toUpdate);
        } else {
            return assessmentRepository.save(assessment);
        }
    }

    @Transactional(readOnly = true)
    public Page<AssessmentResponseDto> getAssessmentsForJob(UUID jobId, Pageable pageable) {
        jobService.getJobEntity(jobId); // validates workspace access
        return assessmentRepository.findByJobIdOrderByOverallScoreDesc(jobId, pageable)
                .map(AssessmentResponseDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public AssessmentResponseDto getAssessmentForCandidateAndJob(UUID candidateId, UUID jobId) {
        jobService.getJobEntity(jobId); // validates workspace access
        UUID workspaceId = SecurityUtils.getCurrentWorkspaceId();
        candidateRepository.findByIdAndWorkspaceId(candidateId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));

        CandidateJobAssessment assessment = assessmentRepository.findByJobIdAndCandidateId(jobId, candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("No assessment found for candidate " + candidateId + " and job " + jobId));

        return AssessmentResponseDto.fromEntity(assessment);
    }
}
