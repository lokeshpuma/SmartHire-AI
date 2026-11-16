package com.example.resumescreening.auth;

import com.example.resumescreening.security.UserRole;
import java.util.UUID;

public class UserProfileDto {
    private UUID id;
    private String name;
    private String email;
    private UserRole role;
    private UUID workspaceId;
    private String workspaceName;

    public UserProfileDto() {}

    public UserProfileDto(UUID id, String name, String email, UserRole role, UUID workspaceId, String workspaceName) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.workspaceId = workspaceId;
        this.workspaceName = workspaceName;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }

    public String getWorkspaceName() { return workspaceName; }
    public void setWorkspaceName(String workspaceName) { this.workspaceName = workspaceName; }
}
