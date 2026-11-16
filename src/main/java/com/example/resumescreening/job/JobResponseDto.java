package com.example.resumescreening.job;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class JobResponseDto {
    private UUID id;
    private UUID workspaceId;
    private String title;
    private String department;
    private String description;
    private JobStatus status;
    private UUID createdBy;
    private List<JobRequirementDto> requirements;
    private Instant createdAt;
    private Instant updatedAt;

    public JobResponseDto() {}

    public static JobResponseDto fromEntity(Job job) {
        JobResponseDto dto = new JobResponseDto();
        dto.setId(job.getId());
        dto.setWorkspaceId(job.getWorkspace().getId());
        dto.setTitle(job.getTitle());
        dto.setDepartment(job.getDepartment());
        dto.setDescription(job.getDescription());
        dto.setStatus(job.getStatus());
        dto.setCreatedBy(job.getCreatedBy());
        if (job.getRequirements() != null) {
            dto.setRequirements(job.getRequirements().stream()
                    .map(JobRequirementDto::fromEntity)
                    .collect(Collectors.toList()));
        }
        dto.setCreatedAt(job.getCreatedAt());
        dto.setUpdatedAt(job.getUpdatedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public List<JobRequirementDto> getRequirements() { return requirements; }
    public void setRequirements(List<JobRequirementDto> requirements) { this.requirements = requirements; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
