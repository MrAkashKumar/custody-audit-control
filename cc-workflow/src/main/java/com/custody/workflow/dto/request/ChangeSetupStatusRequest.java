package com.custody.workflow.dto.request;

import com.custody.core.enums.OperationalStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeSetupStatusRequest(
    @Min(0) long version,
    @NotNull OperationalStatus status,
    @NotBlank @Size(max = 1000) String reason) {}
