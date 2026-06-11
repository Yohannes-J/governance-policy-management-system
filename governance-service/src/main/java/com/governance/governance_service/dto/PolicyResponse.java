package com.governance.governance_service.dto;

import com.governance.governance_service.entity.PolicyStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;


@Data
public class PolicyResponse {
    private Long id;
    private String title;
    private String description;
    private PolicyStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
}
