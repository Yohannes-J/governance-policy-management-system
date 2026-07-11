package com.governance.governance_service.outbox;

import com.governance.governance_service.event.GovernanceEvent;
import com.governance.governance_service.kafka.GovernanceEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxRepository;
    private final GovernanceEventProducer eventProducer;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxRepository.findByPublishedFalseOrderByCreatedAtAsc();

        if (pending.isEmpty()) return;

        log.info("Outbox: found {} unpublished event(s)", pending.size());

        for (OutboxEvent outboxEvent : pending) {
            try {
                GovernanceEvent event = GovernanceEvent.builder()
                        .eventType(outboxEvent.getEventType())
                        .policyId(outboxEvent.getPolicyId())
                        .actor(outboxEvent.getActor())
                        .timestamp(outboxEvent.getTimestamp())
                        .build();

                eventProducer.publishEvent(event);
                outboxEvent.setPublished(true);
                outboxRepository.save(outboxEvent);

                log.info("Outbox: published event id={} type='{}'",
                        outboxEvent.getId(), outboxEvent.getEventType());

            } catch (Exception ex) {
                log.error("Outbox: failed to publish event id={}: {}",
                        outboxEvent.getId(), ex.getMessage());
            }
        }
    }
}
