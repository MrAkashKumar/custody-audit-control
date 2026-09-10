package com.custody.reporting.dto.request;

import com.custody.reporting.enums.ReportFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateScheduleRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank String category,
    @NotBlank String feature,
    @NotBlank String scope,
    @NotNull ReportFormat format,
    @Min(1) @Max(365) int intervalDays,
    String audienceGroup) {}
