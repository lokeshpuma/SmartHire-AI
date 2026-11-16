package com.example.resumescreening.candidate;

import com.example.resumescreening.workspace.Workspace;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "candidate_reference", nullable = false, length = 100)
    private String candidateReference;

    @Column(name = "display_name")
    private String displayName;

    private String email;

    private String phone;

    @Column(name = "source")
    private String source;

    @Column(name = "current_stage")
    private String currentStage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Candidate() {
        this.id = UUID.randomUUID();
        this.currentStage = "SCREENING";
        this.source = "DIRECT_UPLOAD";
        this.createdAt = Instant.now();
    }

    public Candidate(Workspace workspace, String candidateReference, String displayName, String email, String phone) {
        this();
        this.workspace = workspace;
        this.candidateReference = candidateReference;
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Workspace getWorkspace() { return workspace; }
    public void setWorkspace(Workspace workspace) { this.workspace = workspace; }

    public String getCandidateReference() { return candidateReference; }
    public void setCandidateReference(String candidateReference) { this.candidateReference = candidateReference; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
