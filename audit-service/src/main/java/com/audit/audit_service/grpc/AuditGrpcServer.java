package com.audit.audit_service.grpc;

import com.audit.audit_service.entity.AuditLog;
import com.audit.audit_service.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditGrpcServer {

    private final AuditLogRepository auditLogRepository;

    public void recordAudit(String eventType, Long policyId, String actor, String timestamp) {
        log.info("gRPC server stub received: eventType='{}' policyId={} actor='{}'",
                eventType, policyId, actor);

        AuditLog auditLog = AuditLog.builder()
                .eventType(eventType)
                .policyId(policyId)
                .actor(actor)
                .timestamp(LocalDateTime.parse(timestamp))
                .build();

        auditLogRepository.save(auditLog);
        log.info("Audit log saved via gRPC stub for policyId={}", policyId);
    }
}
