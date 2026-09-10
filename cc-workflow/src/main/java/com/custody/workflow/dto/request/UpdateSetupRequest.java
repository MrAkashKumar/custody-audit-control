package com.custody.workflow.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record UpdateSetupRequest(
    @Min(0) long version,
    @NotNull Map<String, String> values,
    @NotBlank @Size(max = 1000) String reason) {}
