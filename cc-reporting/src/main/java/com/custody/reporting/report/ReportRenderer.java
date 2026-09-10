package com.custody.reporting.report;

import com.custody.audit.dto.response.AuditEventResponse;
import com.custody.reporting.enums.ReportFormat;
import java.util.List;

public interface ReportRenderer {
  ReportFormat format();

  byte[] render(List<AuditEventResponse> events, String criteria);
}
