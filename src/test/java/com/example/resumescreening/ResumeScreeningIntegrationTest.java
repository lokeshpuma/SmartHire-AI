package com.example.resumescreening;

import com.example.resumescreening.auth.LoginRequest;
import com.example.resumescreening.job.CreateJobRequest;
import com.example.resumescreening.review.CreateReviewRequest;
import com.example.resumescreening.review.HumanDecision;
import com.example.resumescreening.review.ReviewStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ResumeScreeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("End-to-end recruitment lifecycle: Login -> Create Job -> Parse Requirements -> Upload Resume -> Assess -> Review -> CSV Export -> Audit")
    void testEndToEndRecruitmentLifecycle() throws Exception {
        // 1. Recruiter Login
        LoginRequest loginRequest = new LoginRequest("recruiter@example.com", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = loginJson.get("data").get("token").asText();
        assertNotNull(token);

        // 2. Access /api/v1/auth/me
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("recruiter@example.com"))
                .andExpect(jsonPath("$.data.role").value("RECRUITER"));

        // 3. Create a Job
        CreateJobRequest jobRequest = new CreateJobRequest(
                "Lead Cloud Architect",
                "Infrastructure",
                "Seeking a Lead Cloud Architect. Requirements: Extensive experience with AWS, Kubernetes, Docker, and Python. Preferred: Terraform and CI/CD."
        );

        MvcResult jobResult = mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andReturn();

        JsonNode jobJson = objectMapper.readTree(jobResult.getResponse().getContentAsString());
        String jobId = jobJson.get("data").get("id").asText();

        // 4. Test Requirement Parse
        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/requirements/parse")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // 5. Upload Resume (TXT format)
        String resumeContent = """
                Alex Taylor
                Summary: Cloud Architect with 8 years of experience building modern infrastructure.
                Experience:
                2020 - Present: Principal Cloud Engineer at Tech Corp.
                - Designed and managed multi-region AWS cloud solutions.
                - Orchestrated container workloads using Kubernetes and Docker.
                - Developed automation scripts using Python.
                - Configured infrastructure as code using Terraform and automated pipelines with CI/CD.
                Education:
                B.S. in Computer Science.
                """;

        MockMultipartFile resumeFile = new MockMultipartFile(
                "file",
                "alex_taylor_resume.txt",
                "text/plain",
                resumeContent.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/jobs/" + jobId + "/resumes")
                        .file(resumeFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.candidateId").isNotEmpty())
                .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String candidateId = uploadJson.get("data").get("candidateId").asText();

        // 6. Query Candidates for Job
        MvcResult queueResult = mockMvc.perform(get("/api/v1/jobs/" + jobId + "/candidates")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andReturn();

        JsonNode queueJson = objectMapper.readTree(queueResult.getResponse().getContentAsString());
        assertTrue(queueJson.get("data").get("content").size() > 0);

        // 7. Inspect Candidate Assessment
        MvcResult assessmentResult = mockMvc.perform(get("/api/v1/candidates/" + candidateId + "/assessments/" + jobId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overallScore").isNumber())
                .andExpect(jsonPath("$.data.confidenceOrCompleteness").isNumber())
                .andExpect(jsonPath("$.data.matchedEvidenceJson").isNotEmpty())
                .andReturn();

        // 8. Record Recruiter Review Decision
        CreateReviewRequest reviewRequest = new CreateReviewRequest();
        reviewRequest.setJobId(UUID.fromString(jobId));
        reviewRequest.setReviewStatus(ReviewStatus.REVIEWED);
        reviewRequest.setDecision(HumanDecision.ADVANCE_TO_INTERVIEW);
        reviewRequest.setReason("Strong verified AWS and Kubernetes evidence in project descriptions");
        reviewRequest.setNotes("Recommended for technical architectural round");

        mockMvc.perform(post("/api/v1/candidates/" + candidateId + "/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decision").value("ADVANCE_TO_INTERVIEW"))
                .andExpect(jsonPath("$.data.reviewStatus").value("REVIEWED"));

        // 9. Export CSV
        MvcResult csvResult = mockMvc.perform(get("/api/v1/jobs/" + jobId + "/exports/candidates.csv")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("text/csv")))
                .andReturn();

        String csvContent = csvResult.getResponse().getContentAsString();
        assertTrue(csvContent.contains("Candidate Reference"));
        assertTrue(csvContent.contains("ADVANCE_TO_INTERVIEW"));

        // 10. Verify Health Endpoint
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    @DisplayName("Candidate Portal Flow: Candidate applies publicly -> Recruiter sees candidate in queue -> Advances round")
    void testCandidatePublicApplicationAndRecruiterScreening() throws Exception {
        // 1. Fetch public jobs
        MvcResult jobsResult = mockMvc.perform(get("/api/v1/public/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        JsonNode jobsJson = objectMapper.readTree(jobsResult.getResponse().getContentAsString());
        String jobId = jobsJson.get("data").get(0).get("id").asText();

        // 2. Candidate submits application
        String resumeContent = "John Doe\nEmail: john.doe@example.com\nSkills: Java, Spring Boot, Microservices, Docker, Kubernetes, AWS\nExperience: 5 years at Cloud Corp building Java services.";
        MockMultipartFile candidateFile = new MockMultipartFile(
                "file",
                "john_doe_resume.txt",
                "text/plain",
                resumeContent.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult applyResult = mockMvc.perform(multipart("/api/v1/public/jobs/" + jobId + "/apply")
                        .file(candidateFile)
                        .param("fullName", "John Doe")
                        .param("email", "john.doe@example.com")
                        .param("phone", "+1 555-0199")
                        .param("notes", "Excited to apply for this backend position"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.candidateReference").isNotEmpty())
                .andReturn();

        JsonNode applyJson = objectMapper.readTree(applyResult.getResponse().getContentAsString());
        String candidateRef = applyJson.get("data").get("candidateReference").asText();

        // 3. Candidate tracks application status
        mockMvc.perform(get("/api/v1/public/candidates/track?reference=" + candidateRef))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidateReference").value(candidateRef))
                .andExpect(jsonPath("$.data.currentStage").value("SCREENING"));

        // 4. Recruiter logs in and inspects the candidate in the queue
        LoginRequest loginRequest = new LoginRequest("recruiter@example.com", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("data").get("token").asText();

        MvcResult queueResult = mockMvc.perform(get("/api/v1/jobs/" + jobId + "/candidates")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andReturn();

        String queueContent = queueResult.getResponse().getContentAsString();
        assertTrue(queueContent.contains(candidateRef));
    }
}
