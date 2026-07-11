package com.audit.audit_service.saga;

import com.audit.audit_service.event.GovernanceEvent;
import com.audit.audit_service.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaEventConsumer {

    private final AuditLogService auditLogService;
    private final KafkaTemplate<String, Object> sagaKafkaTemplate;

    @Value("${kafka.topic.audit-recorded:audit-recorded}")
    private String auditRecordedTopic;

    @Value("${kafka.topic.audit-failed:audit-failed}")
    private String auditFailedTopic;

    @KafkaListener(
            topics = "${kafka.topic.governance-events}",
            groupId = "saga-audit-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleGovernanceEvent(GovernanceEvent event) {
        log.info("[Saga] Processing event type='{}' policyId={}", event.getEventType(), event.getPolicyId());

        try {
            auditLogService.saveAuditLog(event);

            SagaEvent successEvent = SagaEvent.builder()
                    .eventType("audit-recorded")
                    .policyId(event.getPolicyId())
                    .originalEventType(event.getEventType())
                    .actor(event.getActor())
                    .success(true)
                    .message("Audit log recorded successfully")
                    .build();

            sagaKafkaTemplate.send(auditRecordedTopic, String.valueOf(event.getPolicyId()), successEvent);
            log.info("[Saga] Audit recorded for policyId={}, published to '{}'",
                    event.getPolicyId(), auditRecordedTopic);

        } catch (Exception ex) {
            log.error("[Saga] Failed to record audit for policyId={}: {}",
                    event.getPolicyId(), ex.getMessage());

            SagaEvent failureEvent = SagaEvent.builder()
                    .eventType("audit-failed")
                    .policyId(event.getPolicyId())
                    .originalEventType(event.getEventType())
                    .actor(event.getActor())
                    .success(false)
                    .message("Audit recording failed: " + ex.getMessage())
                    .build();

            sagaKafkaTemplate.send(auditFailedTopic, String.valueOf(event.getPolicyId()), failureEvent);
            log.warn("[Saga] Compensation event published for policyId={}", event.getPolicyId());
        }
    }
}
