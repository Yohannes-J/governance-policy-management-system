package com.governance.governance_service.grpc;

import com.audit.audit_service.grpc.AuditServiceGrpc;
import com.audit.audit_service.grpc.GetAuditLogsRequest;
import com.audit.audit_service.grpc.GetAuditLogsResponse;
import com.audit.audit_service.grpc.RecordAuditRequest;
import com.audit.audit_service.grpc.RecordAuditResponse;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class AuditGrpcClient {

    // Channel name matches grpc.client.audit-service.* in application.properties
    @GrpcClient("audit-service")
    private AuditServiceGrpc.AuditServiceBlockingStub auditServiceStub;

    /**
     * Records an audit event in the audit-service via gRPC.
     *
     * @return true if the audit-service confirmed success
     */
    public boolean recordAudit(String eventType, Long policyId, String actor) {
        log.info("[gRPC] RecordAudit → eventType='{}' policyId={} actor='{}'",
                eventType, policyId, actor);
        try {
            RecordAuditRequest request = RecordAuditRequest.newBuilder()
                    .setEventType(eventType)
                    .setPolicyId(policyId)
                    .setActor(actor)
                    .setTimestamp(LocalDateTime.now().toString())
                    .build();

            RecordAuditResponse response = auditServiceStub.recordAudit(request);
            log.info("[gRPC] RecordAudit ← auditId={} success={} message='{}'",
                    response.getAuditId(), response.getSuccess(), response.getMessage());
            return response.getSuccess();

        } catch (Exception ex) {
            log.error("[gRPC] RecordAudit failed for policyId={}: {}", policyId, ex.getMessage());
            return false;
        }
    }

    /**
     * Fetches all audit logs for a given policy from the audit-service via gRPC.
     */
    public GetAuditLogsResponse getAuditLogsForPolicy(Long policyId) {
        log.info("[gRPC] GetAuditLogsByPolicy → policyId={}", policyId);
        try {
            GetAuditLogsRequest request = GetAuditLogsRequest.newBuilder()
                    .setPolicyId(policyId)
                    .build();

            GetAuditLogsResponse response = auditServiceStub.getAuditLogsByPolicy(request);
            log.info("[gRPC] GetAuditLogsByPolicy ← {} logs for policyId={}",
                    response.getLogsCount(), policyId);
            return response;

        } catch (Exception ex) {
            log.error("[gRPC] GetAuditLogsByPolicy failed for policyId={}: {}", policyId, ex.getMessage());
            return GetAuditLogsResponse.newBuilder().build();
        }
    }
}
