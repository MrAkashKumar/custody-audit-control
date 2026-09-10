package com.custody.workflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record CreateSetupRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{2,60}") String businessKey,
    @NotBlank String scope,
    @NotNull Map<String, String> values,
    @NotBlank @Size(max = 1000) String reason) {}
