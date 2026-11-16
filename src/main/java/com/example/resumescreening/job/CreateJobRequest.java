package com.example.resumescreening.job;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class CreateJobRequest {

    @NotBlank(message = "Job title is required")
    private String title;

    private String department;

    @NotBlank(message = "Job description is required")
    private String description;

    private List<JobRequirementDto> requirements;

    public CreateJobRequest() {}

    public CreateJobRequest(String title, String department, String description) {
        this.title = title;
        this.department = department;
        this.description = description;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<JobRequirementDto> getRequirements() { return requirements; }
    public void setRequirements(List<JobRequirementDto> requirements) { this.requirements = requirements; }
}
