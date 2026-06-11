package com.governance.governance_service.exception;

import com.governance.governance_service.entity.PolicyStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(PolicyStatus current, PolicyStatus target) {
        super("Cannot transition policy from " + current + " to " + target);
    }
}
