package com.governance.governance_service.saga;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaEvent {
    private String eventType;
    private Long policyId;
    private String originalEventType;
    private String actor;
    private boolean success;
    private String message;
}
