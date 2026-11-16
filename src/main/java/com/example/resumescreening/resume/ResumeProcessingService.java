package com.example.resumescreening.resume;

import com.example.resumescreening.audit.AuditService;
import com.example.resumescreening.candidate.Candidate;
import com.example.resumescreening.candidate.CandidateRepository;
import com.example.resumescreening.common.ResourceNotFoundException;
import com.example.resumescreening.common.ValidationException;
import com.example.resumescreening.document.*;
import com.example.resumescreening.extraction.*;
import com.example.resumescreening.job.Job;
import com.example.resumescreening.job.JobService;
import com.example.resumescreening.matching.AssessmentResponseDto;
import com.example.resumescreening.matching.CandidateJobAssessment;
import com.example.resumescreening.matching.CandidateJobAssessmentRepository;
import com.example.resumescreening.matching.MatchingEngine;
import com.example.resumescreening.review.CandidateReview;
import com.example.resumescreening.review.CandidateReviewRepository;
import com.example.resumescreening.security.SecurityUtils;
import com.example.resumescreening.workspace.Workspace;
import com.example.resumescreening.workspace.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResumeProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ResumeProcessingService.class);

    private final DocumentValidator documentValidator;
    private final MalwareScanner malwareScanner;
    private final StorageService storageService;
    private final DocumentTextExtractor textExtractor;
    private final ResumeProfileExtractor profileExtractor;
    private final MatchingEngine matchingEngine;
    private final CandidateRepository candidateRepository;
    private final ResumeDocumentRepository documentRepository;
    private final ResumeProfileRepository profileRepository;
    private final CandidateJobAssessmentRepository assessmentRepository;
    private final CandidateReviewRepository reviewRepository;
    private final ExtractedEvidenceRepository evidenceRepository;
    private final WorkspaceRepository workspaceRepository;
    private final JobService jobService;
    private final AuditService auditService;
    private final TransactionTemplate transactionTemplate;

    public ResumeProcessingService(
            DocumentValidator documentValidator,
            MalwareScanner malwareScanner,
            StorageService storageService,
            DocumentTextExtractor textExtractor,
            ResumeProfileExtractor profileExtractor,
            MatchingEngine matchingEngine,
            CandidateRepository candidateRepository,
            ResumeDocumentRepository documentRepository,
            ResumeProfileRepository profileRepository,
            CandidateJobAssessmentRepository assessmentRepository,
            CandidateReviewRepository reviewRepository,
            ExtractedEvidenceRepository evidenceRepository,
            WorkspaceRepository workspaceRepository,
            JobService jobService,
            AuditService auditService,
            PlatformTransactionManager transactionManager) {
        this.documentValidator = documentValidator;
        this.malwareScanner = malwareScanner;
        this.storageService = storageService;
        this.textExtractor = textExtractor;
        this.profileExtractor = profileExtractor;
        this.matchingEngine = matchingEngine;
        this.candidateRepository = candidateRepository;
        this.documentRepository = documentRepository;
        this.profileRepository = profileRepository;
        this.assessmentRepository = assessmentRepository;
        this.reviewRepository = reviewRepository;
        this.evidenceRepository = evidenceRepository;
        this.workspaceRepository = workspaceRepository;
        this.jobService = jobService;
        this.auditService = auditService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public ResumeUploadResponse processSingleResume(UUID jobId, MultipartFile file) {
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "resume.pdf";

        try {
            return transactionTemplate.execute(status -> doProcessResume(jobId, file, originalFilename));
        } catch (ValidationException e) {
            log.warn("Validation failed for {}: {}", originalFilename, e.getMessage());
            return ResumeUploadResponse.failed(originalFilename, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error processing resume {}: {}", originalFilename, e.getMessage(), e);
            return ResumeUploadResponse.failed(originalFilename, "Processing error: " + e.getMessage());
        }
    }

    private ResumeUploadResponse doProcessResume(UUID jobId, MultipartFile file, String originalFilename) {
        try {
            byte[] fileBytes = file.getBytes();

            // 1. Validation & Hash
            DocumentValidator.ValidationResult valResult = documentValidator.validateAndHash(originalFilename, fileBytes);

            // 2. Malware Scan
            MalwareScanner.ScanResult scan = malwareScanner.scan(fileBytes, originalFilename);
            if (!scan.isClean()) {
                auditService.logCurrentAction("MALWARE_BLOCKED", "Upload", originalFilename, "{\"threat\":\"" + scan.getDetails() + "\"}");
                throw new ValidationException("Malware detection failed: " + scan.getDetails());
            }

            // 3. Duplicate check via SHA-256
            Optional<ResumeDocument> duplicate = documentRepository.findByFileHashAndDeletedAtIsNull(valResult.getSha256());
            if (duplicate.isPresent()) {
                ResumeDocument existingDoc = duplicate.get();
                Candidate existingCand = existingDoc.getCandidate();
                log.info("Duplicate resume detected with SHA-256 {}. Candidate reference: {}", valResult.getSha256(), existingCand.getCandidateReference());

                // If jobId provided, run assessment if not already assessed
                if (jobId != null) {
                    Job job = jobService.getPublicJobEntity(jobId);
                    profileRepository.findByCandidateIdWithEvidence(existingCand.getId()).ifPresent(prof -> {
                        Optional<CandidateJobAssessment> existingAss = assessmentRepository.findByJobIdAndCandidateId(jobId, existingCand.getId());
                        if (existingAss.isEmpty()) {
                            CandidateJobAssessment ass = matchingEngine.assessCandidate(job, prof);
                            assessmentRepository.save(ass);
                        }
                    });
                }

                return new ResumeUploadResponse(true, originalFilename, existingDoc.getId(), existingCand.getId(),
                        existingCand.getCandidateReference(), "Duplicate document detected; linked to existing candidate reference", valResult.getSha256());
            }

            // 4. Safe Storage
            String storageKey = storageService.store(new ByteArrayInputStream(fileBytes), valResult.getExtension());

            // 5. Text Extraction
            DocumentTextExtractor.ExtractionResult extResult = textExtractor.extractText(fileBytes, valResult.getMimeType(), originalFilename);

            // 6. Create Candidate
            UUID workspaceId = SecurityUtils.getCurrentWorkspaceId();
            Workspace workspace = workspaceRepository.findById(workspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

            String candRef = "CAND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String displayName = deriveDisplayName(originalFilename, extResult.getNormalizedFullText());

            Candidate candidate = new Candidate(workspace, candRef, displayName, null, null);
            candidate.setSource("RECRUITER_UPLOAD");
            candidate.setCurrentStage("SCREENING");
            candidate = candidateRepository.save(candidate);

            // 7. Create ResumeDocument
            ResumeDocument document = new ResumeDocument(candidate, storageKey, originalFilename, valResult.getMimeType(), valResult.getSha256());
            document.setExtractionStatus(ExtractionStatus.EXTRACTED);
            document.setParserVersion(extResult.getParserName() + ":" + extResult.getParserVersion());
            document = documentRepository.save(document);

            // 8. Extract Profile and Evidence
            ResumeProfile profile = profileExtractor.extractProfile(candidate, document, extResult);
            profile = profileRepository.save(profile);

            // 9. Run Assessment if JobId is provided
            if (jobId != null) {
                Job job = jobService.getPublicJobEntity(jobId);
                CandidateJobAssessment assessment = matchingEngine.assessCandidate(job, profile);
                assessmentRepository.save(assessment);
            }

            auditService.logCurrentAction("RESUME_PROCESSED", "ResumeDocument", document.getId().toString(),
                    "{\"candidateRef\":\"" + candRef + "\",\"filename\":\"" + originalFilename + "\"}");

            return ResumeUploadResponse.success(originalFilename, document.getId(), candidate.getId(), candRef, valResult.getSha256());

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public BatchUploadResponse processBatchResumes(UUID jobId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ValidationException("No files provided for batch upload");
        }

        List<ResumeUploadResponse> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (MultipartFile file : files) {
            ResumeUploadResponse resp = processSingleResume(jobId, file);
            results.add(resp);
            if (resp.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
            }
        }

        auditService.logCurrentAction("BATCH_RESUME_UPLOAD", "Job", jobId != null ? jobId.toString() : "GLOBAL",
                "{\"total\":" + files.size() + ",\"success\":" + successCount + ",\"failed\":" + failureCount + "}");

        return new BatchUploadResponse(files.size(), successCount, failureCount, results);
    }

    public JobService getJobService() {
        return jobService;
    }

    public ResumeUploadResponse processCandidateApplication(UUID jobId, MultipartFile file, String fullName, String email, String phone, String notes) {
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "resume.pdf";

        try {
            return transactionTemplate.execute(status -> doProcessCandidateApplication(jobId, file, originalFilename, fullName, email, phone, notes));
        } catch (ValidationException e) {
            log.warn("Candidate application validation failed for {}: {}", originalFilename, e.getMessage());
            return ResumeUploadResponse.failed(originalFilename, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error processing candidate application {}: {}", originalFilename, e.getMessage(), e);
            return ResumeUploadResponse.failed(originalFilename, "Processing error: " + e.getMessage());
        }
    }

    private ResumeUploadResponse doProcessCandidateApplication(UUID jobId, MultipartFile file, String originalFilename, String fullName, String email, String phone, String notes) {
        try {
            Job job = jobService.getPublicJobEntity(jobId);
            Workspace workspace = job.getWorkspace();

            byte[] fileBytes = file.getBytes();
            DocumentValidator.ValidationResult valResult = documentValidator.validateAndHash(originalFilename, fileBytes);
            MalwareScanner.ScanResult scan = malwareScanner.scan(fileBytes, originalFilename);
            if (!scan.isClean()) {
                auditService.recordEvent(workspace.getId(), null, email != null ? email : "candidate-portal", "MALWARE_BLOCKED", "Upload", originalFilename, "{\"threat\":\"" + scan.getDetails() + "\"}");
                throw new ValidationException("Malware detection failed: " + scan.getDetails());
            }

            Optional<ResumeDocument> duplicate = documentRepository.findByFileHashAndDeletedAtIsNull(valResult.getSha256());
            Candidate candidate;
            ResumeDocument document;
            ResumeProfile profile;

            if (duplicate.isPresent()) {
                document = duplicate.get();
                candidate = document.getCandidate();
                if (fullName != null && !fullName.isBlank()) candidate.setDisplayName(fullName);
                if (email != null && !email.isBlank()) candidate.setEmail(email);
                if (phone != null && !phone.isBlank()) candidate.setPhone(phone);
                candidate = candidateRepository.save(candidate);

                profile = profileRepository.findByCandidateIdWithEvidence(candidate.getId()).orElse(null);
            } else {
                String storageKey = storageService.store(new ByteArrayInputStream(fileBytes), valResult.getExtension());
                DocumentTextExtractor.ExtractionResult extResult = textExtractor.extractText(fileBytes, valResult.getMimeType(), originalFilename);

                String candRef = "CAND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                String displayName = (fullName != null && !fullName.isBlank()) ? fullName : deriveDisplayName(originalFilename, extResult.getNormalizedFullText());

                candidate = new Candidate(workspace, candRef, displayName, email, phone);
                candidate.setSource("CANDIDATE_PORTAL");
                candidate.setCurrentStage("SCREENING");
                candidate = candidateRepository.save(candidate);

                document = new ResumeDocument(candidate, storageKey, originalFilename, valResult.getMimeType(), valResult.getSha256());
                document.setExtractionStatus(ExtractionStatus.EXTRACTED);
                document.setParserVersion(extResult.getParserName() + ":" + extResult.getParserVersion());
                document = documentRepository.save(document);

                profile = profileExtractor.extractProfile(candidate, document, extResult);
                profile = profileRepository.save(profile);
            }

            if (profile != null) {
                Optional<CandidateJobAssessment> existingAss = assessmentRepository.findByJobIdAndCandidateId(jobId, candidate.getId());
                if (existingAss.isEmpty()) {
                    CandidateJobAssessment assessment = matchingEngine.assessCandidate(job, profile);
                    assessmentRepository.save(assessment);
                }
            }

            auditService.recordEvent(workspace.getId(), null, email != null ? email : "candidate-portal", "CANDIDATE_APPLICATION_SUBMITTED", "Candidate", candidate.getId().toString(),
                    "{\"candidateRef\":\"" + candidate.getCandidateReference() + "\",\"jobId\":\"" + jobId + "\",\"jobTitle\":\"" + job.getTitle() + "\"}");

            return ResumeUploadResponse.success(originalFilename, document.getId(), candidate.getId(), candidate.getCandidateReference(), valResult.getSha256());
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional(readOnly = true)
    public CandidateResponseDto trackCandidateApplication(String reference) {
        String query = reference != null ? reference.trim() : "";
        Candidate candidate = candidateRepository.findByCandidateReference(query)
                .orElseGet(() -> candidateRepository.findFirstByEmailOrderByCreatedAtDesc(query)
                        .orElseThrow(() -> new ResourceNotFoundException("No application found with reference or email: " + query)));

        CandidateResponseDto dto = CandidateResponseDto.fromEntity(candidate);
        profileRepository.findByCandidateId(candidate.getId()).ifPresent(p -> {
            dto.setProfile(ResumeProfileDto.fromEntity(p));
            dto.setReviewStatus(p.getReviewStatus());
        });

        List<CandidateJobAssessment> assessments = assessmentRepository.findByCandidateId(candidate.getId());
        if (!assessments.isEmpty()) {
            CandidateJobAssessment ass = assessments.get(0);
            dto.setAssessment(AssessmentResponseDto.fromEntity(ass));
            reviewRepository.findByCandidateIdAndJobId(candidate.getId(), ass.getJob().getId()).ifPresent(r -> {
                dto.setDecision(r.getDecision() != null ? r.getDecision().name() : null);
            });
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public Page<CandidateResponseDto> getCandidatesForJob(UUID jobId, Pageable pageable) {
        Job job = jobService.getJobEntity(jobId);
        Page<CandidateJobAssessment> assessmentsPage = assessmentRepository.findByJobIdOrderByOverallScoreDesc(jobId, pageable);

        List<CandidateResponseDto> dtoList = new ArrayList<>();
        for (CandidateJobAssessment ass : assessmentsPage.getContent()) {
            Candidate candidate = ass.getCandidate();
            CandidateResponseDto dto = CandidateResponseDto.fromEntity(candidate);
            dto.setAssessment(AssessmentResponseDto.fromEntity(ass));

            profileRepository.findByCandidateId(candidate.getId()).ifPresent(p -> {
                dto.setProfile(ResumeProfileDto.fromEntity(p));
                dto.setReviewStatus(p.getReviewStatus());
            });

            reviewRepository.findByCandidateIdAndJobId(candidate.getId(), jobId).ifPresent(r -> {
                dto.setDecision(r.getDecision() != null ? r.getDecision().name() : null);
            });

            dtoList.add(dto);
        }

        return new PageImpl<>(dtoList, pageable, assessmentsPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public CandidateResponseDto getCandidateById(UUID candidateId) {
        UUID workspaceId = SecurityUtils.getCurrentWorkspaceId();
        Candidate candidate = candidateRepository.findByIdAndWorkspaceId(candidateId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));

        CandidateResponseDto dto = CandidateResponseDto.fromEntity(candidate);
        profileRepository.findByCandidateIdWithEvidence(candidate.getId()).ifPresent(p -> {
            dto.setProfile(ResumeProfileDto.fromEntity(p));
            dto.setReviewStatus(p.getReviewStatus());
        });

        return dto;
    }

    @Transactional(readOnly = true)
    public List<ExtractedEvidenceDto> getCandidateEvidence(UUID candidateId) {
        UUID workspaceId = SecurityUtils.getCurrentWorkspaceId();
        candidateRepository.findByIdAndWorkspaceId(candidateId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));

        ResumeProfile profile = profileRepository.findByCandidateId(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("No profile found for candidate: " + candidateId));

        return evidenceRepository.findByResumeProfileId(profile.getId()).stream()
                .map(ExtractedEvidenceDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteCandidateResume(UUID candidateId) {
        UUID workspaceId = SecurityUtils.getCurrentWorkspaceId();
        Candidate candidate = candidateRepository.findByIdAndWorkspaceId(candidateId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));

        // Delete physical files
        List<ResumeDocument> docs = documentRepository.findByCandidateId(candidateId);
        for (ResumeDocument doc : docs) {
            storageService.delete(doc.getStorageKey());
            doc.setDeletedAt(Instant.now());
            documentRepository.save(doc);
        }

        // Delete assessments, reviews and profile
        assessmentRepository.deleteByCandidateId(candidateId);
        reviewRepository.deleteByCandidateId(candidateId);

        profileRepository.findByCandidateId(candidateId).ifPresent(profileRepository::delete);

        auditService.logCurrentAction("DELETE_CANDIDATE_RESUME", "Candidate", candidateId.toString(),
                "{\"candidateRef\":\"" + candidate.getCandidateReference() + "\",\"retentionPolicyApplied\":true}");
    }

    public Resource loadResumeResource(UUID candidateId) throws IOException {
        UUID workspaceId = SecurityUtils.getCurrentWorkspaceId();
        Candidate candidate = candidateRepository.findByIdAndWorkspaceId(candidateId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));

        ResumeDocument doc = documentRepository.findByCandidateIdAndDeletedAtIsNull(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("No active resume document for candidate: " + candidateId));

        auditService.logCurrentAction("DOWNLOAD_RESUME", "ResumeDocument", doc.getId().toString(),
                "{\"candidateRef\":\"" + candidate.getCandidateReference() + "\"}");

        return new InputStreamResource(storageService.load(doc.getStorageKey()));
    }

    public String getResumeOriginalFilename(UUID candidateId) {
        UUID workspaceId = SecurityUtils.getCurrentWorkspaceId();
        Candidate candidate = candidateRepository.findByIdAndWorkspaceId(candidateId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));
        return documentRepository.findByCandidateIdAndDeletedAtIsNull(candidateId)
                .map(ResumeDocument::getOriginalFilename)
                .orElse("resume.pdf");
    }

    private String deriveDisplayName(String filename, String text) {
        // Strip extension from filename for fallback name
        String name = filename;
        if (name.contains(".")) {
            name = name.substring(0, name.lastIndexOf('.'));
        }
        name = name.replaceAll("[_-]", " ").trim();

        // Attempt to find a 2-3 word header on first line
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() > 2 && trimmed.length() < 40 && trimmed.matches("^[a-zA-Z\\s.'-]+$")) {
                return trimmed;
            }
        }

        return name.length() > 0 ? name : "Candidate";
    }
}
