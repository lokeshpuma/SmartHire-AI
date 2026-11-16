package com.example.resumescreening.audit;

import com.example.resumescreening.security.SecurityUtils;
import com.example.resumescreening.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEvent(UUID workspaceId, UUID actorId, String actorEmail, String action, String resourceType, String resourceId, String sanitizedMetadataJson) {
        try {
            AuditEvent event = new AuditEvent(
                    workspaceId,
                    actorId,
                    actorEmail,
                    action,
                    resourceType,
                    resourceId,
                    sanitizedMetadataJson
            );
            auditEventRepository.save(event);
            log.info("AUDIT: workspace={} actor={} action={} resource={}/{}", workspaceId, actorEmail, action, resourceType, resourceId);
        } catch (Exception ex) {
            log.error("Failed to record audit event: {}", ex.getMessage());
        }
    }

    public void logCurrentAction(String action, String resourceType, String resourceId, String sanitizedMetadataJson) {
        try {
            UserPrincipal user = SecurityUtils.getCurrentUser();
            recordEvent(user.getWorkspaceId(), user.getId(), user.getEmail(), action, resourceType, resourceId, sanitizedMetadataJson);
        } catch (Exception e) {
            log.debug("No authenticated user for audit: {}", e.getMessage());
        }
    }
}
