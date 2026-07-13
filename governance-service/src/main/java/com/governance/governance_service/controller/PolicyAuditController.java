package com.governance.governance_service.controller;

import com.audit.audit_service.grpc.AuditLogEntry;
import com.audit.audit_service.grpc.GetAuditLogsResponse;
import com.governance.governance_service.grpc.AuditGrpcClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/policies/{id}/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Policy Audit", description = "Audit log access for a policy via gRPC to audit-service")
public class PolicyAuditController {

    private final AuditGrpcClient auditGrpcClient;

    /**
     * Fetches audit logs for a policy by calling audit-service over gRPC.
     */
    @GetMapping
    @Operation(summary = "Get audit logs for a policy via gRPC")
    public ResponseEntity<List<Map<String, Object>>> getAuditLogs(
            @Parameter(description = "Policy ID") @PathVariable Long id) {

        GetAuditLogsResponse response = auditGrpcClient.getAuditLogsForPolicy(id);

        List<Map<String, Object>> logs = response.getLogsList().stream()
                .map(this::entryToMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(logs);
    }

    /**
     * Records an audit event for a policy by calling audit-service over gRPC.
     * Useful for manual audit triggers or testing the gRPC channel.
     */
    @PostMapping
    @Operation(summary = "Record an audit event for a policy via gRPC")
    public ResponseEntity<Map<String, Object>> recordAudit(
            @Parameter(description = "Policy ID") @PathVariable Long id,
            @RequestParam(defaultValue = "policy-action") String eventType,
            @RequestParam(defaultValue = "system") String actor) {

        boolean success = auditGrpcClient.recordAudit(eventType, id, actor);

        return ResponseEntity.ok(Map.of(
                "policyId", id,
                "eventType", eventType,
                "actor", actor,
                "recorded", success
        ));
    }

    private Map<String, Object> entryToMap(AuditLogEntry entry) {
        return Map.of(
                "id", entry.getId(),
                "eventType", entry.getEventType(),
                "policyId", entry.getPolicyId(),
                "actor", entry.getActor(),
                "timestamp", entry.getTimestamp()
        );
    }
}
