package com.governance.governance_service.grpc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class AuditGrpcClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${audit.service.url:http://localhost:8082}")
    private String auditServiceUrl;

    public List<?> getAuditLogsForPolicy(Long policyId) {
        String url = auditServiceUrl + "/audit-logs/policy/" + policyId;
        log.info("[Audit Client] Fetching audit logs for policyId={}", policyId);
        try {
            Object[] response = restTemplate.getForObject(url, Object[].class);
            if (response == null) return Collections.emptyList();
            log.info("[Audit Client] Received {} logs for policyId={}", response.length, policyId);
            return Arrays.asList(response);
        } catch (RestClientException ex) {
            log.error("[Audit Client] Failed to fetch audit logs for policyId={}: {}", policyId, ex.getMessage());
            return Collections.emptyList();
        }
    }
}
