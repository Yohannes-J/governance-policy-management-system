package com.audit.audit_service.service;


import com.audit.audit_service.dto.AuditLogResponse;
import com.audit.audit_service.entity.AuditLog;
import com.audit.audit_service.event.GovernanceEvent;
import com.audit.audit_service.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;


    @Transactional
    public void saveAuditLog(GovernanceEvent event) {
        AuditLog auditLog = AuditLog.builder()
                .eventType(event.getEventType())
                .policyId(event.getPolicyId())
                .actor(event.getActor())
                .timestamp(event.getTimestamp())
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("Saved audit log id={} eventType='{}' policyId={} actor='{}'",
                saved.getId(), saved.getEventType(), saved.getPolicyId(), saved.getActor());
    }


    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAllAuditLogs() {
        return auditLogRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }


    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogsByPolicyId(Long policyId) {
        return auditLogRepository.findByPolicyIdOrderByTimestampDesc(policyId).stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .eventType(log.getEventType())
                .policyId(log.getPolicyId())
                .actor(log.getActor())
                .timestamp(log.getTimestamp())
                .build();
    }
}
