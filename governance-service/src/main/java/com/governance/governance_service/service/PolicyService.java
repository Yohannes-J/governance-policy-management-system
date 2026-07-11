package com.governance.governance_service.service;

import com.governance.governance_service.dto.CreatePolicyRequest;
import com.governance.governance_service.dto.PolicyResponse;
import com.governance.governance_service.entity.Policy;
import com.governance.governance_service.entity.PolicyStatus;
import com.governance.governance_service.exception.InvalidStatusTransitionException;
import com.governance.governance_service.exception.PolicyNotFoundException;
import com.governance.governance_service.outbox.OutboxEvent;
import com.governance.governance_service.outbox.OutboxEventRepository;
import com.governance.governance_service.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public PolicyResponse createPolicy(CreatePolicyRequest request) {
        Policy policy = Policy.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .createdBy(request.getCreatedBy())
                .status(PolicyStatus.DRAFT)
                .build();

        Policy saved = policyRepository.save(policy);
        log.info("Created policy id={} title='{}' by '{}'", saved.getId(), saved.getTitle(), saved.getCreatedBy());
        saveOutboxEvent("policy-created", saved.getId(), saved.getCreatedBy());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PolicyResponse> getAllPolicies() {
        return policyRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PolicyResponse getPolicyById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public PolicyResponse submitPolicy(Long id) {
        Policy policy = findOrThrow(id);
        if (policy.getStatus() != PolicyStatus.DRAFT) {
            throw new InvalidStatusTransitionException(policy.getStatus(), PolicyStatus.PENDING_APPROVAL);
        }
        policy.setStatus(PolicyStatus.PENDING_APPROVAL);
        Policy saved = policyRepository.save(policy);
        log.info("Submitted policy id={} for approval", id);
        saveOutboxEvent("policy-submitted", saved.getId(), saved.getCreatedBy());
        return toResponse(saved);
    }

    @Transactional
    public PolicyResponse approvePolicy(Long id, String actor) {
        Policy policy = findOrThrow(id);
        if (policy.getStatus() != PolicyStatus.PENDING_APPROVAL) {
            throw new InvalidStatusTransitionException(policy.getStatus(), PolicyStatus.APPROVED);
        }
        policy.setStatus(PolicyStatus.APPROVED);
        Policy saved = policyRepository.save(policy);
        log.info("Approved policy id={} by '{}'", id, actor);
        saveOutboxEvent("policy-approved", saved.getId(), actor);
        return toResponse(saved);
    }

    @Transactional
    public PolicyResponse rejectPolicy(Long id, String actor) {
        Policy policy = findOrThrow(id);
        if (policy.getStatus() != PolicyStatus.PENDING_APPROVAL) {
            throw new InvalidStatusTransitionException(policy.getStatus(), PolicyStatus.REJECTED);
        }
        policy.setStatus(PolicyStatus.REJECTED);
        Policy saved = policyRepository.save(policy);
        log.info("Rejected policy id={} by '{}'", id, actor);
        saveOutboxEvent("policy-rejected", saved.getId(), actor);
        return toResponse(saved);
    }

    private Policy findOrThrow(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new PolicyNotFoundException(id));
    }

    private void saveOutboxEvent(String eventType, Long policyId, String actor) {
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventType(eventType)
                .policyId(policyId)
                .actor(actor)
                .timestamp(LocalDateTime.now())
                .build();
        outboxEventRepository.save(outboxEvent);
        log.info("[Outbox] Saved outbox event type='{}' policyId={}", eventType, policyId);
    }

    private PolicyResponse toResponse(Policy policy) {
        return PolicyResponse.builder()
                .id(policy.getId())
                .title(policy.getTitle())
                .description(policy.getDescription())
                .status(policy.getStatus())
                .createdBy(policy.getCreatedBy())
                .createdAt(policy.getCreatedAt())
                .build();
    }
}
