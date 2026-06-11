package com.audit.audit_service.repository;

import com.audit.audit_service.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog,Long> {
    List<AuditLog> findByPolicyIdOrderByTimestampDesc(Long policyId);
}
