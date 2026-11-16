package com.example.resumescreening.audit;

import com.example.resumescreening.common.ApiResponse;
import com.example.resumescreening.common.PagedResponse;
import com.example.resumescreening.common.ResourceNotFoundException;
import com.example.resumescreening.security.SecurityUtils;
import com.example.resumescreening.taxonomy.Skill;
import com.example.resumescreening.taxonomy.SkillDto;
import com.example.resumescreening.taxonomy.SkillTaxonomyService;
import com.example.resumescreening.workspace.Workspace;
import com.example.resumescreening.workspace.WorkspaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class AdminController {

    private final AuditEventRepository auditEventRepository;
    private final SkillTaxonomyService taxonomyService;
    private final WorkspaceRepository workspaceRepository;
    private final AuditService auditService;

    public AdminController(AuditEventRepository auditEventRepository,
                           SkillTaxonomyService taxonomyService,
                           WorkspaceRepository workspaceRepository,
                           AuditService auditService) {
        this.auditEventRepository = auditEventRepository;
        this.taxonomyService = taxonomyService;
        this.workspaceRepository = workspaceRepository;
        this.auditService = auditService;
    }

    @GetMapping("/admin/audit-events")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<PagedResponse<AuditEvent>>> getAuditEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        UUID workspaceId = SecurityUtils.getCurrentWorkspaceId();
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditEvent> eventPage = auditEventRepository.findByWorkspaceIdOrderByTimestampDesc(workspaceId, pageable);

        PagedResponse<AuditEvent> pagedResponse = new PagedResponse<>(
                eventPage.getContent(),
                eventPage.getNumber(),
                eventPage.getSize(),
                eventPage.getTotalElements(),
                eventPage.getTotalPages(),
                eventPage.isLast()
        );

        return ResponseEntity.ok(ApiResponse.ok(pagedResponse));
    }

    @GetMapping("/admin/skills")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<List<SkillDto>>> getTaxonomySkills() {
        List<SkillDto> skills = taxonomyService.getAllSkills().stream()
                .map(SkillDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(skills));
    }

    @PostMapping("/admin/skills")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<SkillDto>> createSkill(@RequestBody SkillDto dto) {
        Skill created = taxonomyService.addCustomSkill(dto.getCanonicalName(), dto.getCategory(), dto.getDescription(), dto.getAliases());
        auditService.logCurrentAction("CREATE_TAXONOMY_SKILL", "Skill", created.getId().toString(),
                "{\"canonicalName\":\"" + created.getCanonicalName() + "\"}");
        return ResponseEntity.ok(ApiResponse.ok("Skill registered in taxonomy", SkillDto.fromEntity(created)));
    }

    @GetMapping("/admin/workspaces")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<Workspace>> getWorkspaceSettings() {
        UUID workspaceId = SecurityUtils.getCurrentWorkspaceId();
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        return ResponseEntity.ok(ApiResponse.ok(workspace));
    }

    @PutMapping("/admin/workspaces/{workspaceId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<Workspace>> updateWorkspaceSettings(
            @PathVariable UUID workspaceId,
            @RequestBody Workspace update) {
        SecurityUtils.validateWorkspaceAccess(workspaceId);
        Workspace ws = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        if (update.getName() != null && !update.getName().isBlank()) {
            ws.setName(update.getName());
        }
        if (update.getRetentionPolicy() != null) {
            ws.setRetentionPolicy(update.getRetentionPolicy());
        }
        if (update.getSettingsJson() != null) {
            ws.setSettingsJson(update.getSettingsJson());
        }

        Workspace saved = workspaceRepository.save(ws);
        auditService.logCurrentAction("UPDATE_WORKSPACE_POLICY", "Workspace", saved.getId().toString(),
                "{\"retentionPolicy\":\"" + saved.getRetentionPolicy() + "\"}");

        return ResponseEntity.ok(ApiResponse.ok("Workspace updated", saved));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealth() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString(),
                "version", "1.0.0",
                "engine", "Intelligent Resume Screening Engine Java",
                "framework", "Spring Boot 3.3.5 / Java 21",
                "responsible_ai_safeguards", "ACTIVE"
        );
        return ResponseEntity.ok(ApiResponse.ok(health));
    }
}
