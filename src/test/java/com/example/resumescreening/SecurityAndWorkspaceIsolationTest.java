package com.example.resumescreening;

import com.example.resumescreening.auth.LoginRequest;
import com.example.resumescreening.auth.User;
import com.example.resumescreening.auth.UserRepository;
import com.example.resumescreening.candidate.Candidate;
import com.example.resumescreening.candidate.CandidateRepository;
import com.example.resumescreening.job.Job;
import com.example.resumescreening.job.JobRepository;
import com.example.resumescreening.security.UserRole;
import com.example.resumescreening.workspace.Workspace;
import com.example.resumescreening.workspace.WorkspaceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAndWorkspaceIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Enforces workspace isolation: User from Workspace 1 cannot access Workspace 2 jobs")
    void testWorkspaceIsolation() throws Exception {
        // Create an isolated Workspace 2
        Workspace ws2 = new Workspace("Isolated Workspace Beta");
        ws2 = workspaceRepository.save(ws2);

        User userWs2 = new User(ws2, "Beta Recruiter", "beta@example.com", passwordEncoder.encode("password123"), UserRole.RECRUITER);
        userRepository.save(userWs2);

        Job jobWs2 = new Job(ws2, "Secret Data Scientist", "Data", "Python, Spark, ML", userWs2.getId());
        jobWs2 = jobRepository.save(jobWs2);

        // Login as Recruiter from Workspace 1
        String tokenWs1 = authenticate("recruiter@example.com", "password123");

        // Attempt to access Job from Workspace 2 using Workspace 1 token -> Must be rejected (404 / 403)
        mockMvc.perform(get("/api/v1/jobs/" + jobWs2.getId())
                        .header("Authorization", "Bearer " + tokenWs1))
                .andExpect(status().isNotFound()); // ResourceNotFoundException because scoped query findByIdAndWorkspaceId
    }

    @Test
    @DisplayName("Role-based access: Recruiter is forbidden from accessing Admin-only audit endpoints")
    void testRbacEnforcement() throws Exception {
        String recruiterToken = authenticate("recruiter@example.com", "password123");

        mockMvc.perform(get("/api/v1/admin/audit-events")
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin user can access Admin audit endpoints")
    void testAdminAccessAllowed() throws Exception {
        String adminToken = authenticate("admin@example.com", "admin123");

        mockMvc.perform(get("/api/v1/admin/audit-events")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("Unauthenticated requests to secured endpoints return 401/403")
    void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isForbidden());
    }

    private String authenticate(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest(email, password);
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        return json.get("data").get("token").asText();
    }
}
