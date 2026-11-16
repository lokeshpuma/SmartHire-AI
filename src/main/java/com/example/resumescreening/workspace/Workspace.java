package com.example.resumescreening.workspace;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspaces")
public class Workspace {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "settings_json", columnDefinition = "TEXT")
    private String settingsJson;

    @Column(name = "retention_policy")
    private String retentionPolicy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Workspace() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.retentionPolicy = "standard-365-days";
    }

    public Workspace(String name) {
        this();
        this.name = name;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSettingsJson() { return settingsJson; }
    public void setSettingsJson(String settingsJson) { this.settingsJson = settingsJson; }

    public String getRetentionPolicy() { return retentionPolicy; }
    public void setRetentionPolicy(String retentionPolicy) { this.retentionPolicy = retentionPolicy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
