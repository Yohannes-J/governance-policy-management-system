package com.audit.audit_service.kafka;

import com.audit.audit_service.event.GovernanceEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GovernanceEventConsumer {

    public void consume(GovernanceEvent event) {
        log.debug("GovernanceEventConsumer (disabled) received: {}", event.getEventType());
    }
}
