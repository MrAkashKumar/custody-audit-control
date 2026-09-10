package com.custody.reporting.dto.response;

import com.custody.reporting.enums.ReportFormat;
import com.custody.reporting.enums.ReportStatus;
import java.time.Instant;

public record ReportResponse(
    String id,
    String owner,
    String audienceGroup,
    String trigger,
    ReportFormat format,
    ReportStatus status,
    long rowCount,
    Instant requestedAt,
    Instant expiresAt,
    String failureCode,
    String traceId) {}
