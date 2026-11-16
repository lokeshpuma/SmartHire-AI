package com.example.resumescreening.job;

import com.example.resumescreening.audit.AuditService;
import com.example.resumescreening.common.ResourceNotFoundException;
import com.example.resumescreening.security.SecurityUtils;
import com.example.resumescreening.security.UserPrincipal;
import com.example.resumescreening.taxonomy.Skill;
import com.example.resumescreening.taxonomy.SkillTaxonomyService;
import com.example.resumescreening.workspace.Workspace;
import com.example.resumescreening.workspace.WorkspaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobRequirementRepository requirementRepository;
    private final WorkspaceRepository workspaceRepository;
    private final SkillTaxonomyService taxonomyService;
    private final AuditService auditService;

    public JobService(JobRepository jobRepository,
                      JobRequirementRepository requirementRepository,
                      WorkspaceRepository workspaceRepository,
                      SkillTaxonomyService taxonomyService,
                      AuditService auditService) {
        this.jobRepository = jobRepository;
        this.requirementRepository = requirementRepository;
        this.workspaceRepository = workspaceRepository;
        this.taxonomyService = taxonomyService;
        this.auditService = auditService;
    }

    @Transactional
    public JobResponseDto createJob(CreateJobRequest request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        Workspace workspace = workspaceRepository.findById(user.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        Job job = new Job(workspace, request.getTitle(), request.getDepartment(), request.getDescription(), user.getId());

        if (request.getRequirements() != null && !request.getRequirements().isEmpty()) {
            for (JobRequirementDto reqDto : request.getRequirements()) {
                JobRequirement req = new JobRequirement(
                        job,
                        reqDto.getCanonicalSkillId(),
                        reqDto.getRequirementText(),
                        reqDto.getRequirementType() != null ? reqDto.getRequirementType() : RequirementType.REQUIRED,
                        reqDto.getWeight() > 0 ? reqDto.getWeight() : 1.0
                );
                req.setAcceptableSynonyms(reqDto.getAcceptableSynonyms());
                req.setEvidenceGuidance(reqDto.getEvidenceGuidance());
                req.setActive(reqDto.isActive());
                job.addRequirement(req);
            }
        } else {
            // Automatically parse initial requirements from the description
            List<JobRequirementDto> parsed = parseRequirementsFromText(request.getDescription(), job);
            for (JobRequirementDto reqDto : parsed) {
                JobRequirement req = new JobRequirement(
                        job,
                        reqDto.getCanonicalSkillId(),
                        reqDto.getRequirementText(),
                        reqDto.getRequirementType(),
                        reqDto.getWeight()
                );
                req.setAcceptableSynonyms(reqDto.getAcceptableSynonyms());
                req.setEvidenceGuidance(reqDto.getEvidenceGuidance());
                req.setActive(true);
                job.addRequirement(req);
            }
        }

        Job saved = jobRepository.save(job);
        auditService.logCurrentAction("CREATE_JOB", "Job", saved.getId().toString(), "{\"title\":\"" + saved.getTitle() + "\"}");

        return JobResponseDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<JobResponseDto> getJobs(Pageable pageable) {
        UUID workspaceId = SecurityUtils.getCurrentWorkspaceId();
        return jobRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId, pageable)
                .map(JobResponseDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<JobResponseDto> getPublicJobs() {
        return jobRepository.findAll().stream()
                .filter(j -> j.getStatus() == JobStatus.OPEN)
                .map(JobResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public JobResponseDto getJobById(UUID jobId) {
        Job job = getJobEntity(jobId);
        return JobResponseDto.fromEntity(job);
    }

    @Transactional
    public JobResponseDto updateJob(UUID jobId, UpdateJobRequest request) {
        Job job = getJobEntity(jobId);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            job.setTitle(request.getTitle());
        }
        if (request.getDepartment() != null) {
            job.setDepartment(request.getDepartment());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            job.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            job.setStatus(request.getStatus());
        }
        job.setUpdatedAt(Instant.now());

        if (request.getRequirements() != null) {
            updateJobRequirements(job, request.getRequirements());
        }

        Job saved = jobRepository.save(job);
        auditService.logCurrentAction("UPDATE_JOB", "Job", saved.getId().toString(), "{\"status\":\"UPDATED\"}");

        return JobResponseDto.fromEntity(saved);
    }

    @Transactional
    public List<JobRequirementDto> parseRequirementsForJob(UUID jobId, ParseRequirementsRequest request) {
        Job job = getJobEntity(jobId);
        String textToParse = (request != null && request.getCustomText() != null && !request.getCustomText().isBlank())
                ? request.getCustomText()
                : job.getDescription();

        return parseRequirementsFromText(textToParse, job);
    }

    @Transactional
    public List<JobRequirementDto> setJobRequirements(UUID jobId, List<JobRequirementDto> requirementDtos) {
        Job job = getJobEntity(jobId);
        updateJobRequirements(job, requirementDtos);
        Job saved = jobRepository.save(job);

        auditService.logCurrentAction("UPDATE_JOB_REQUIREMENTS", "Job", jobId.toString(),
                "{\"requirementCount\":" + requirementDtos.size() + "}");

        return saved.getRequirements().stream()
                .map(JobRequirementDto::fromEntity)
                .collect(Collectors.toList());
    }

    public Job getJobEntity(UUID jobId) {
        UUID workspaceId = SecurityUtils.getCurrentWorkspaceId();
        return jobRepository.findByIdAndWorkspaceId(jobId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));
    }

    public Job getPublicJobEntity(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));
    }

    private void updateJobRequirements(Job job, List<JobRequirementDto> dts) {
        job.getRequirements().clear();
        for (JobRequirementDto dto : dts) {
            JobRequirement req = new JobRequirement(
                    job,
                    dto.getCanonicalSkillId(),
                    dto.getRequirementText(),
                    dto.getRequirementType() != null ? dto.getRequirementType() : RequirementType.REQUIRED,
                    dto.getWeight() > 0 ? dto.getWeight() : 1.0
            );
            req.setAcceptableSynonyms(dto.getAcceptableSynonyms());
            req.setEvidenceGuidance(dto.getEvidenceGuidance());
            req.setActive(dto.isActive());
            job.addRequirement(req);
        }
    }

    private List<JobRequirementDto> parseRequirementsFromText(String text, Job job) {
        if (text == null || text.isBlank()) return Collections.emptyList();

        List<JobRequirementDto> result = new ArrayList<>();
        Set<String> seenSkills = new HashSet<>();

        // Section splitting
        String[] lines = text.split("\\r?\\n");
        RequirementType currentType = RequirementType.REQUIRED;

        Pattern prefPattern = Pattern.compile("(?i)(nice to have|preferred|bonus|plus|optional)");
        Pattern reqPattern = Pattern.compile("(?i)(requirements?|must have|qualifications?|required)");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (prefPattern.matcher(trimmed).find()) {
                currentType = RequirementType.PREFERRED;
                continue;
            } else if (reqPattern.matcher(trimmed).find()) {
                currentType = RequirementType.REQUIRED;
                continue;
            }

            // Extract skills from this line
            List<SkillTaxonomyService.SkillMatch> matches = taxonomyService.extractSkillsFromText(trimmed);
            for (SkillTaxonomyService.SkillMatch match : matches) {
                Skill skill = match.getSkill();
                if (seenSkills.add(skill.getCanonicalName())) {
                    JobRequirementDto dto = new JobRequirementDto();
                    dto.setCanonicalSkillId(skill.getCanonicalName());
                    dto.setRequirementText(trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed);
                    dto.setRequirementType(currentType);
                    dto.setWeight(currentType == RequirementType.REQUIRED ? 1.0 : 0.6);
                    dto.setAcceptableSynonyms(skill.getAliases() != null ?
                            skill.getAliases().stream().map(a -> a.getAlias()).collect(Collectors.joining(", ")) : "");
                    dto.setEvidenceGuidance("Look for project experience, roles, or explicit achievements involving " + skill.getCanonicalName());
                    dto.setActive(true);
                    result.add(dto);
                }
            }
        }

        // If no structured skills detected, extract any general skill found in whole text
        if (result.isEmpty()) {
            List<SkillTaxonomyService.SkillMatch> allMatches = taxonomyService.extractSkillsFromText(text);
            for (SkillTaxonomyService.SkillMatch match : allMatches) {
                Skill skill = match.getSkill();
                if (seenSkills.add(skill.getCanonicalName())) {
                    JobRequirementDto dto = new JobRequirementDto();
                    dto.setCanonicalSkillId(skill.getCanonicalName());
                    dto.setRequirementText("Demonstrated proficiency in " + skill.getCanonicalName());
                    dto.setRequirementType(RequirementType.REQUIRED);
                    dto.setWeight(1.0);
                    dto.setActive(true);
                    result.add(dto);
                }
            }
        }

        return result;
    }
}
