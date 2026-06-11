package com.governance.governance_service.controller;

import com.governance.governance_service.dto.CreatePolicyRequest;
import com.governance.governance_service.dto.PolicyResponse;
import com.governance.governance_service.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
@Tag(name = "Policies", description = "Governance policy lifecycle management")
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping
    @Operation(summary = "Create a new governance policy")
    public ResponseEntity<PolicyResponse> createPolicy(@Valid @RequestBody CreatePolicyRequest request) {
        PolicyResponse response = policyService.createPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all governance policies")
    public ResponseEntity<List<PolicyResponse>> getAllPolicies() {
        return ResponseEntity.ok(policyService.getAllPolicies());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a governance policy by ID")
    public ResponseEntity<PolicyResponse> getPolicyById(
            @Parameter(description = "Policy ID") @PathVariable Long id) {
        return ResponseEntity.ok(policyService.getPolicyById(id));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit a policy for approval (DRAFT -> PENDING_APPROVAL)")
    public ResponseEntity<PolicyResponse> submitPolicy(
            @Parameter(description = "Policy ID") @PathVariable Long id) {
        return ResponseEntity.ok(policyService.submitPolicy(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a policy (PENDING_APPROVAL -> APPROVED)")
    public ResponseEntity<PolicyResponse> approvePolicy(
            @Parameter(description = "Policy ID") @PathVariable Long id,
            @Parameter(description = "Actor performing the approval")
            @RequestParam(defaultValue = "manager") String actor) {
        return ResponseEntity.ok(policyService.approvePolicy(id, actor));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a policy (PENDING_APPROVAL -> REJECTED)")
    public ResponseEntity<PolicyResponse> rejectPolicy(
            @Parameter(description = "Policy ID") @PathVariable Long id,
            @Parameter(description = "Actor performing the rejection")
            @RequestParam(defaultValue = "manager") String actor) {
        return ResponseEntity.ok(policyService.rejectPolicy(id, actor));
    }
}