package com.custody.reporting.report.impl;

import com.custody.audit.dto.response.AuditEventResponse;
import com.custody.reporting.enums.ReportFormat;
import com.custody.reporting.report.ReportRenderer;
import com.custody.reporting.utils.ReportColumns;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

@Component
class CsvRenderer implements ReportRenderer {
  public ReportFormat format() {
    return ReportFormat.CSV;
  }

  public byte[] render(List<AuditEventResponse> events, String criteria) {
    try {
      StringWriter out = new StringWriter();
      try (CSVPrinter csv = new CSVPrinter(out, CSVFormat.DEFAULT)) {
        csv.printRecord("Selection criteria", ReportColumns.safe(criteria));
        csv.printRecord("Result", events.isEmpty() ? "No activity" : "Events: " + events.size());
        csv.printRecord(ReportColumns.HEADERS);
        for (AuditEventResponse auditEventResponse : events)
          for (var row : ReportColumns.rows(auditEventResponse))
            csv.printRecord(row.stream().map(ReportColumns::safe).toList());
      }
      return out.toString().getBytes(StandardCharsets.UTF_8);
    } catch (IOException iOException) {
      throw new IllegalStateException("Report rendering failed", iOException);
    }
  }
}
