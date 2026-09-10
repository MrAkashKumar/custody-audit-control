package com.custody.workflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApprovalDecisionRequest(boolean approve, @NotBlank @Size(max = 1000) String reason) {}
