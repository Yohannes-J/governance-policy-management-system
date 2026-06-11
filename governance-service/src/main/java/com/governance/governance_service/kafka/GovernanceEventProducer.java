package com.governance.governance_service.kafka;

import com.governance.governance_service.event.GovernanceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;


@Slf4j
@Component
@RequiredArgsConstructor
public class GovernanceEventProducer {
    private final KafkaTemplate<String, GovernanceEvent> kafkaTemplate;

    @Value("${kafka.topic.governance-events}")
    private String topic;

    public void publishEvent(GovernanceEvent event) {
        CompletableFuture<SendResult<String, GovernanceEvent>> future =
                kafkaTemplate.send(topic, String.valueOf(event.getPolicyId()), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event [{}] for policyId={}: {}",
                        event.getEventType(), event.getPolicyId(), ex.getMessage());
            } else {
                log.info("Published event [{}] for policyId={} to topic={} partition={} offset={}",
                        event.getEventType(),
                        event.getPolicyId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });

    }
}
