package com.governance.governance_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class CreatePolicyRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "CreatedBy is required")
    private String createdBy;
}
