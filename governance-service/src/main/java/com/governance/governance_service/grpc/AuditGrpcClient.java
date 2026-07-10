package com.governance.governance_service.grpc;

import com.audit.audit_service.grpc.AuditLogEntry;
import com.audit.audit_service.grpc.AuditServiceGrpc;
import com.audit.audit_service.grpc.GetAuditLogsRequest;
import com.audit.audit_service.grpc.GetAuditLogsResponse;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AuditGrpcClient {

    @GrpcClient("audit-service")
    private AuditServiceGrpc.AuditServiceBlockingStub auditServiceStub;

    public List<AuditLogEntry> getAuditLogsForPolicy(Long policyId) {
        log.info("[gRPC] Calling audit-service for policyId={}", policyId);
        try {
            GetAuditLogsRequest request = GetAuditLogsRequest.newBuilder()
                    .setPolicyId(policyId)
                    .build();

            GetAuditLogsResponse response = auditServiceStub.getAuditLogsByPolicy(request);
            log.info("[gRPC] Received {} logs for policyId={}", response.getLogsCount(), policyId);
            return response.getLogsList();

        } catch (StatusRuntimeException ex) {
            log.error("[gRPC] Failed to fetch audit logs for policyId={}: {} - {}",
                    policyId, ex.getStatus().getCode(), ex.getMessage());
            return List.of();
        }
    }
}
