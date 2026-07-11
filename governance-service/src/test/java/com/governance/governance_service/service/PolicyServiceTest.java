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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private PolicyService policyService;

    private Policy draftPolicy;
    private Policy pendingPolicy;

    @BeforeEach
    void setUp() {
        draftPolicy = Policy.builder()
                .id(1L)
                .title("Test Policy")
                .description("Test Description")
                .createdBy("alice")
                .status(PolicyStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .build();

        pendingPolicy = Policy.builder()
                .id(2L)
                .title("Pending Policy")
                .description("Pending Description")
                .createdBy("bob")
                .status(PolicyStatus.PENDING_APPROVAL)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ─── createPolicy ────────────────────────────────────────────────────────

    @Test
    void createPolicy_savesPolicy_andOutboxEvent() {
        CreatePolicyRequest request = new CreatePolicyRequest();
        request.setTitle("Test Policy");
        request.setDescription("Test Description");
        request.setCreatedBy("alice");

        when(policyRepository.save(any(Policy.class))).thenReturn(draftPolicy);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(new OutboxEvent());

        PolicyResponse response = policyService.createPolicy(request);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Test Policy");
        assertThat(response.getStatus()).isEqualTo(PolicyStatus.DRAFT);
        assertThat(response.getCreatedBy()).isEqualTo("alice");

        verify(policyRepository, times(1)).save(any(Policy.class));
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
    }

    // ─── getAllPolicies ───────────────────────────────────────────────────────

    @Test
    void getAllPolicies_returnsAllPolicies() {
        when(policyRepository.findAll()).thenReturn(List.of(draftPolicy, pendingPolicy));

        List<PolicyResponse> result = policyService.getAllPolicies();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStatus()).isEqualTo(PolicyStatus.DRAFT);
        assertThat(result.get(1).getStatus()).isEqualTo(PolicyStatus.PENDING_APPROVAL);
    }

    @Test
    void getAllPolicies_returnsEmptyList_whenNoPolicies() {
        when(policyRepository.findAll()).thenReturn(List.of());

        List<PolicyResponse> result = policyService.getAllPolicies();

        assertThat(result).isEmpty();
    }

    // ─── getPolicyById ────────────────────────────────────────────────────────

    @Test
    void getPolicyById_returnsPolicy_whenExists() {
        when(policyRepository.findById(1L)).thenReturn(Optional.of(draftPolicy));

        PolicyResponse response = policyService.getPolicyById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Test Policy");
    }

    @Test
    void getPolicyById_throwsNotFoundException_whenNotExists() {
        when(policyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.getPolicyById(99L))
                .isInstanceOf(PolicyNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ─── submitPolicy ─────────────────────────────────────────────────────────

    @Test
    void submitPolicy_transitionsDraftToPending() {
        Policy savedPolicy = Policy.builder()
                .id(1L).title("Test Policy").description("Desc")
                .createdBy("alice").status(PolicyStatus.PENDING_APPROVAL)
                .createdAt(LocalDateTime.now()).build();

        when(policyRepository.findById(1L)).thenReturn(Optional.of(draftPolicy));
        when(policyRepository.save(any(Policy.class))).thenReturn(savedPolicy);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(new OutboxEvent());

        PolicyResponse response = policyService.submitPolicy(1L);

        assertThat(response.getStatus()).isEqualTo(PolicyStatus.PENDING_APPROVAL);
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    void submitPolicy_throwsException_whenNotInDraft() {
        when(policyRepository.findById(2L)).thenReturn(Optional.of(pendingPolicy));

        assertThatThrownBy(() -> policyService.submitPolicy(2L))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("PENDING_APPROVAL");
    }

    // ─── approvePolicy ────────────────────────────────────────────────────────

    @Test
    void approvePolicy_transitionsPendingToApproved() {
        Policy approvedPolicy = Policy.builder()
                .id(2L).title("Pending Policy").description("Desc")
                .createdBy("bob").status(PolicyStatus.APPROVED)
                .createdAt(LocalDateTime.now()).build();

        when(policyRepository.findById(2L)).thenReturn(Optional.of(pendingPolicy));
        when(policyRepository.save(any(Policy.class))).thenReturn(approvedPolicy);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(new OutboxEvent());

        PolicyResponse response = policyService.approvePolicy(2L, "manager");

        assertThat(response.getStatus()).isEqualTo(PolicyStatus.APPROVED);
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    void approvePolicy_throwsException_whenNotPending() {
        when(policyRepository.findById(1L)).thenReturn(Optional.of(draftPolicy));

        assertThatThrownBy(() -> policyService.approvePolicy(1L, "manager"))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("DRAFT");
    }

    // ─── rejectPolicy ─────────────────────────────────────────────────────────

    @Test
    void rejectPolicy_transitionsPendingToRejected() {
        Policy rejectedPolicy = Policy.builder()
                .id(2L).title("Pending Policy").description("Desc")
                .createdBy("bob").status(PolicyStatus.REJECTED)
                .createdAt(LocalDateTime.now()).build();

        when(policyRepository.findById(2L)).thenReturn(Optional.of(pendingPolicy));
        when(policyRepository.save(any(Policy.class))).thenReturn(rejectedPolicy);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(new OutboxEvent());

        PolicyResponse response = policyService.rejectPolicy(2L, "manager");

        assertThat(response.getStatus()).isEqualTo(PolicyStatus.REJECTED);
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    void rejectPolicy_throwsException_whenNotPending() {
        when(policyRepository.findById(1L)).thenReturn(Optional.of(draftPolicy));

        assertThatThrownBy(() -> policyService.rejectPolicy(1L, "manager"))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("DRAFT");
    }
}
