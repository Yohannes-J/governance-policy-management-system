package com.audit.audit_service.grpc;

import com.audit.audit_service.entity.AuditLog;
import com.audit.audit_service.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditGrpcService {

    private final AuditLogRepository auditLogRepository;

    public List<AuditLog> getAuditLogsByPolicy(Long policyId) {
        log.info("[gRPC stub] GetAuditLogsByPolicy called for policyId={}", policyId);
        return auditLogRepository.findByPolicyIdOrderByTimestampDesc(policyId);
    }
}
