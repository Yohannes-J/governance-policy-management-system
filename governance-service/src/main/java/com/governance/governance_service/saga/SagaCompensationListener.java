package com.governance.governance_service.saga;

import com.governance.governance_service.entity.Policy;
import com.governance.governance_service.entity.PolicyStatus;
import com.governance.governance_service.repository.PolicyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaCompensationListener {
    private final PolicyRepository policyRepository;

    @KafkaListener(
            topics = "${kafka.topic.audit-failed:audit-failed}",
            groupId = "governance-saga-compensation-group",
            containerFactory = "sagaKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleAuditFailed(SagaEvent event) {
        log.warn("[Saga Compensation] Received audit-failed for policyId={} originalEvent='{}'",
                event.getPolicyId(), event.getOriginalEventType());

        if ("policy-approved".equals(event.getOriginalEventType())) {
            Optional<Policy> policyOpt = policyRepository.findById(event.getPolicyId());
            if (policyOpt.isPresent()) {
                Policy policy = policyOpt.get();
                PolicyStatus previousStatus = policy.getStatus();
                policy.setStatus(PolicyStatus.PENDING_APPROVAL);
                policyRepository.save(policy);
                log.warn("[Saga Compensation] Reverted policy id={} from {} back to PENDING_APPROVAL. Reason: {}",
                        event.getPolicyId(), previousStatus, event.getMessage());
            }
        } else {
            log.error("[Saga Compensation] Audit failed for event='{}' policyId={}. Manual review required. Reason: {}",
                    event.getOriginalEventType(), event.getPolicyId(), event.getMessage());
        }
    }
}

