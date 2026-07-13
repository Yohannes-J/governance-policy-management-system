package com.governance.governance_service.controller;

import com.governance.governance_service.grpc.AuditGrpcClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/policies/{id}/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Policy Audit", description = "Fetch audit logs for a policy via internal call to audit-service")
public class PolicyAuditController {

    private final AuditGrpcClient auditGrpcClient;

    @GetMapping
    @Operation(summary = "Record an audit event for a policy via gRPC stub")
    public ResponseEntity<Boolean> recordAudit(
            @Parameter(description = "Policy ID") @PathVariable Long id) {
        boolean result = auditGrpcClient.recordAudit("policy-queried", id, "system");
        return ResponseEntity.ok(result);
    }
}
