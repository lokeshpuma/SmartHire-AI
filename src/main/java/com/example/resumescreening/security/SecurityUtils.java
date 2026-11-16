package com.example.resumescreening.security;

import com.example.resumescreening.common.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public class SecurityUtils {

    private SecurityUtils() {}

    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException("User is not authenticated");
    }

    public static UUID getCurrentWorkspaceId() {
        return getCurrentUser().getWorkspaceId();
    }

    public static void validateWorkspaceAccess(UUID resourceWorkspaceId) {
        UserPrincipal currentUser = getCurrentUser();
        // Admins can be scoped to their workspace as well per PRD 4.1: "Enforce workspace isolation for every API and file operation."
        if (!currentUser.getWorkspaceId().equals(resourceWorkspaceId)) {
            throw new AccessDeniedException("Access denied to resources belonging to another workspace");
        }
    }
}
