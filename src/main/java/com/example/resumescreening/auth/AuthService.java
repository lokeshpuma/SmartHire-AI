package com.example.resumescreening.auth;

import com.example.resumescreening.audit.AuditService;
import com.example.resumescreening.common.ValidationException;
import com.example.resumescreening.security.JwtTokenProvider;
import com.example.resumescreening.security.SecurityUtils;
import com.example.resumescreening.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AuditService auditService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.auditService = auditService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        String token = tokenProvider.generateToken(userPrincipal);

        auditService.recordEvent(
                userPrincipal.getWorkspaceId(),
                userPrincipal.getId(),
                userPrincipal.getEmail(),
                "USER_LOGIN",
                "User",
                userPrincipal.getId().toString(),
                "{\"status\":\"SUCCESS\"}"
        );

        return new LoginResponse(
                token,
                userPrincipal.getId(),
                userPrincipal.getName(),
                userPrincipal.getEmail(),
                userPrincipal.getRole(),
                userPrincipal.getWorkspaceId(),
                userPrincipal.getWorkspaceName()
        );
    }

    public void logout() {
        try {
            UserPrincipal user = SecurityUtils.getCurrentUser();
            auditService.logCurrentAction("USER_LOGOUT", "User", user.getId().toString(), "{\"status\":\"LOGGED_OUT\"}");
        } catch (Exception ignored) {
        }
        SecurityContextHolder.clearContext();
    }

    public UserProfileDto getCurrentUserProfile() {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return new UserProfileDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getWorkspaceId(),
                user.getWorkspaceName()
        );
    }
}
