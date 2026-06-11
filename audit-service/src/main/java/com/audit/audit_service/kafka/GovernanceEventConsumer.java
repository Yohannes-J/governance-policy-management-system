package com.audit.audit_service.kafka;


import com.audit.audit_service.event.GovernanceEvent;
import com.audit.audit_service.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class GovernanceEventConsumer {

    private final AuditLogService auditLogService;

    @KafkaListener(
            topics = "${kafka.topic.governance-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(GovernanceEvent event) {
        log.info("Received governance event: type='{}' policyId={} actor='{}'",
                event.getEventType(), event.getPolicyId(), event.getActor());
        auditLogService.saveAuditLog(event);
    }
}
