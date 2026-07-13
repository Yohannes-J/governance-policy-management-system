package com.governance.governance_service.grpc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuditGrpcClient {

    @Value("${grpc.audit-service.host}")
    private String host;

    @Value("${grpc.audit-service.port}")
    private int port;

    public boolean recordAudit(String eventType, Long policyId, String actor) {
        log.info("gRPC stub called: eventType='{}' policyId={} actor='{}' host={}:{}",
                eventType, policyId, actor, host, port);
        return true;
    }
}
