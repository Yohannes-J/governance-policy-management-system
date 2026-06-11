package com.audit.audit_service.controller;


import com.audit.audit_service.dto.AuditLogResponse;
import com.audit.audit_service.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Query governance action audit records")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Get all audit log records")
    public ResponseEntity<List<AuditLogResponse>> getAllAuditLogs() {
        return ResponseEntity.ok(auditLogService.getAllAuditLogs());
    }

    @GetMapping("/policy/{policyId}")
    @Operation(summary = "Get audit logs for a specific policy")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogsByPolicyId(
            @Parameter(description = "Policy ID") @PathVariable Long policyId) {
        return ResponseEntity.ok(auditLogService.getAuditLogsByPolicyId(policyId));
    }
}

