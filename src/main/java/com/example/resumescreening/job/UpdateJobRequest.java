package com.example.resumescreening.job;

import java.util.List;

public class UpdateJobRequest {
    private String title;
    private String department;
    private String description;
    private JobStatus status;
    private List<JobRequirementDto> requirements;

    public UpdateJobRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public List<JobRequirementDto> getRequirements() { return requirements; }
    public void setRequirements(List<JobRequirementDto> requirements) { this.requirements = requirements; }
}
