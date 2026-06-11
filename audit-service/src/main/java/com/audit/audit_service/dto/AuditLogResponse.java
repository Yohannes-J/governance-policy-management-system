package com.audit.audit_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@Builder
public class AuditLogResponse {

    private Long id;
    private String eventType;
    private Long policyId;
    private String actor;
    private LocalDateTime timestamp;
}
