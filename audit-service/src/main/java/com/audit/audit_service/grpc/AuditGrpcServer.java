package com.audit.audit_service.grpc;

import com.audit.audit_service.entity.AuditLog;
import com.audit.audit_service.repository.AuditLogRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Real gRPC server endpoint for the AuditService contract.
 * Extends the generated base class produced from audit.proto.
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AuditGrpcServer extends AuditServiceGrpc.AuditServiceImplBase {

    private final AuditLogRepository auditLogRepository;

    // ─── RecordAudit ────────────────────────────────────────────────────────

    @Override
    public void recordAudit(RecordAuditRequest request,
                            StreamObserver<RecordAuditResponse> responseObserver) {
        log.info("[gRPC] RecordAudit received: eventType='{}' policyId={} actor='{}'",
                request.getEventType(), request.getPolicyId(), request.getActor());
        try {
            AuditLog auditLog = AuditLog.builder()
                    .eventType(request.getEventType())
                    .policyId(request.getPolicyId())
                    .actor(request.getActor())
                    .timestamp(LocalDateTime.parse(request.getTimestamp()))
                    .build();

            AuditLog saved = auditLogRepository.save(auditLog);
            log.info("[gRPC] AuditLog saved: id={} policyId={}", saved.getId(), saved.getPolicyId());

            RecordAuditResponse response = RecordAuditResponse.newBuilder()
                    .setSuccess(true)
                    .setAuditId(saved.getId())
                    .setMessage("Audit recorded successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception ex) {
            log.error("[gRPC] RecordAudit failed for policyId={}: {}", request.getPolicyId(), ex.getMessage());

            RecordAuditResponse errorResponse = RecordAuditResponse.newBuilder()
                    .setSuccess(false)
                    .setAuditId(0)
                    .setMessage("Failed to record audit: " + ex.getMessage())
                    .build();

            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }

    // ─── GetAuditLogsByPolicy ────────────────────────────────────────────────

    @Override
    public void getAuditLogsByPolicy(GetAuditLogsRequest request,
                                     StreamObserver<GetAuditLogsResponse> responseObserver) {
        log.info("[gRPC] GetAuditLogsByPolicy received: policyId={}", request.getPolicyId());
        try {
            List<AuditLog> logs = auditLogRepository
                    .findByPolicyIdOrderByTimestampDesc(request.getPolicyId());

            GetAuditLogsResponse.Builder responseBuilder = GetAuditLogsResponse.newBuilder();

            for (AuditLog auditLog : logs) {
                responseBuilder.addLogs(
                        AuditLogEntry.newBuilder()
                                .setId(auditLog.getId())
                                .setEventType(auditLog.getEventType())
                                .setPolicyId(auditLog.getPolicyId())
                                .setActor(auditLog.getActor())
                                .setTimestamp(auditLog.getTimestamp().toString())
                                .build()
                );
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception ex) {
            log.error("[gRPC] GetAuditLogsByPolicy failed for policyId={}: {}",
                    request.getPolicyId(), ex.getMessage());
            responseObserver.onError(ex);
        }
    }
}
