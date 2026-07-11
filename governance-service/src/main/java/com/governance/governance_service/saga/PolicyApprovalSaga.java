package com.governance.governance_service.saga;


import com.governance.governance_service.entity.Policy;
import com.governance.governance_service.entity.PolicyStatus;
import com.governance.governance_service.outbox.OutboxEvent;
import com.governance.governance_service.outbox.OutboxEventRepository;
import com.governance.governance_service.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyApprovalSaga {

    private final PolicyRepository policyRepository;
    private final OutboxEventRepository outboxRepository;

    @Transactional
    public void startApprovalSaga(Long policyId, String actor) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found: " + policyId));

        if (policy.getStatus() != PolicyStatus.DRAFT) {
            throw new RuntimeException("Policy must be in DRAFT status to start approval saga");
        }

        policy.setStatus(PolicyStatus.PENDING_APPROVAL);
        policyRepository.save(policy);
        publishOutbox("policy-submitted", policyId, actor);
        log.info("Saga Step 1 complete: policy {} submitted by {}", policyId, actor);
    }

    @Transactional
    public void approveInSaga(Long policyId, String actor) {
        try {
            Policy policy = policyRepository.findById(policyId)
                    .orElseThrow(() -> new RuntimeException("Policy not found: " + policyId));

            policy.setStatus(PolicyStatus.APPROVED);
            policyRepository.save(policy);
            publishOutbox("policy-approved", policyId, actor);
            log.info("Saga Step 2 complete: policy {} approved by {}", policyId, actor);

        } catch (Exception ex) {
            log.error("Saga Step 2 failed for policy {}: {}. Running compensation.", policyId, ex.getMessage());
            compensate(policyId, actor, "Approval failed: " + ex.getMessage());
        }
    }

    @Transactional
    public void compensate(Long policyId, String actor, String reason) {
        policyRepository.findById(policyId).ifPresent(policy -> {
            policy.setStatus(PolicyStatus.PENDING_APPROVAL);
            policyRepository.save(policy);
            log.info("Saga compensation: policy {} reverted. Reason: {}", policyId, reason);
        });
        publishOutbox("policy-approval-failed", policyId, actor);
    }

    private void publishOutbox(String eventType, Long policyId, String actor) {
        OutboxEvent outbox = OutboxEvent.builder()
                .eventType(eventType)
                .policyId(policyId)
                .actor(actor)
                .timestamp(LocalDateTime.now())
                .build();
        outboxRepository.save(outbox);
    }
}
