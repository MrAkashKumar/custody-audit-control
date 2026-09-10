package com.custody.reporting.dto.response;

import com.custody.reporting.enums.ReportFormat;
import java.time.Instant;

public record ScheduleResponse(
    String id,
    String name,
    String owner,
    String audienceGroup,
    String category,
    String feature,
    String scope,
    ReportFormat format,
    boolean enabled,
    int intervalDays,
    Instant nextRunAt,
    String lastFailure) {}
