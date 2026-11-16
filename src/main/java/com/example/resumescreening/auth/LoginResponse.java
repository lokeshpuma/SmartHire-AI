package com.example.resumescreening.auth;

import com.example.resumescreening.security.UserRole;
import java.util.UUID;

public class LoginResponse {
    private String token;
    private String tokenType = "Bearer";
    private UUID userId;
    private String name;
    private String email;
    private UserRole role;
    private UUID workspaceId;
    private String workspaceName;

    public LoginResponse() {}

    public LoginResponse(String token, UUID userId, String name, String email, UserRole role, UUID workspaceId, String workspaceName) {
        this.token = token;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.workspaceId = workspaceId;
        this.workspaceName = workspaceName;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

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
