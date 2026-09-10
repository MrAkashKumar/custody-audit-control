package com.custody.reporting.dto.request;

import com.custody.audit.dto.request.AuditSearchRequest;
import com.custody.reporting.enums.ReportFormat;
import jakarta.validation.constraints.NotNull;

public record GenerateReportRequest(
    @NotNull ReportFormat format, @NotNull AuditSearchRequest filter) {}
