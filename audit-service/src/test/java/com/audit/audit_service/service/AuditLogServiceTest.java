package com.audit.audit_service.service;

import com.audit.audit_service.dto.AuditLogResponse;
import com.audit.audit_service.entity.AuditLog;
import com.audit.audit_service.event.GovernanceEvent;
import com.audit.audit_service.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private AuditLog auditLog1;
    private AuditLog auditLog2;
    private GovernanceEvent event;

    @BeforeEach
    void setUp() {
        auditLog1 = AuditLog.builder()
                .id(1L)
                .eventType("policy-created")
                .policyId(10L)
                .actor("alice")
                .timestamp(LocalDateTime.now())
                .build();

        auditLog2 = AuditLog.builder()
                .id(2L)
                .eventType("policy-approved")
                .policyId(10L)
                .actor("manager")
                .timestamp(LocalDateTime.now())
                .build();

        event = new GovernanceEvent(
                "policy-created",
                10L,
                "alice",
                LocalDateTime.now()
        );
    }

    // ─── saveAuditLog ─────────────────────────────────────────────────────────

    @Test
    void saveAuditLog_savesAuditLogFromEvent() {
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(auditLog1);

        auditLogService.saveAuditLog(event);

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void saveAuditLog_mapsAllFieldsCorrectly() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog saved = invocation.getArgument(0);
            assertThat(saved.getEventType()).isEqualTo("policy-created");
            assertThat(saved.getPolicyId()).isEqualTo(10L);
            assertThat(saved.getActor()).isEqualTo("alice");
            assertThat(saved.getTimestamp()).isNotNull();
            return saved;
        });

        auditLogService.saveAuditLog(event);
    }

    // ─── getAllAuditLogs ──────────────────────────────────────────────────────

    @Test
    void getAllAuditLogs_returnsAllRecords() {
        when(auditLogRepository.findAll()).thenReturn(List.of(auditLog1, auditLog2));

        List<AuditLogResponse> result = auditLogService.getAllAuditLogs();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEventType()).isEqualTo("policy-created");
        assertThat(result.get(1).getEventType()).isEqualTo("policy-approved");
    }

    @Test
    void getAllAuditLogs_returnsEmptyList_whenNoRecords() {
        when(auditLogRepository.findAll()).thenReturn(List.of());

        List<AuditLogResponse> result = auditLogService.getAllAuditLogs();

        assertThat(result).isEmpty();
    }

    // ─── getAuditLogsByPolicyId ───────────────────────────────────────────────

    @Test
    void getAuditLogsByPolicyId_returnsLogsForPolicy() {
        when(auditLogRepository.findByPolicyIdOrderByTimestampDesc(10L))
                .thenReturn(List.of(auditLog2, auditLog1));

        List<AuditLogResponse> result = auditLogService.getAuditLogsByPolicyId(10L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEventType()).isEqualTo("policy-approved");
        assertThat(result.get(1).getEventType()).isEqualTo("policy-created");
    }

    @Test
    void getAuditLogsByPolicyId_returnsEmptyList_whenNoneFound() {
        when(auditLogRepository.findByPolicyIdOrderByTimestampDesc(99L))
                .thenReturn(List.of());

        List<AuditLogResponse> result = auditLogService.getAuditLogsByPolicyId(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void getAuditLogsByPolicyId_mapsResponseFieldsCorrectly() {
        when(auditLogRepository.findByPolicyIdOrderByTimestampDesc(10L))
                .thenReturn(List.of(auditLog1));

        List<AuditLogResponse> result = auditLogService.getAuditLogsByPolicyId(10L);

        AuditLogResponse response = result.get(0);
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEventType()).isEqualTo("policy-created");
        assertThat(response.getPolicyId()).isEqualTo(10L);
        assertThat(response.getActor()).isEqualTo("alice");
        assertThat(response.getTimestamp()).isNotNull();
    }
}
