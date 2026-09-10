package com.custody.reporting.utils;

import com.custody.audit.dto.response.AuditEventResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class ReportColumns {
  public static final List<String> HEADERS =
      List.of(
          "Event ID",
          "Schema version",
          "Event type",
          "Timestamp UTC",
          "Category",
          "Feature",
          "Scope",
          "Record",
          "Reference",
          "Action",
          "Status",
          "Actor",
          "Maker",
          "Checker",
          "Reason",
          "Screen",
          "Endpoint",
          "HTTP method",
          "HTTP status",
          "Outcome",
          "Error code",
          "Source IP",
          "User agent",
          "Risk level",
          "Duration ms",
          "Metadata",
          "Field",
          "Before",
          "Proposed",
          "After");

  private ReportColumns() {}

  public static List<List<String>> rows(AuditEventResponse auditEventResponse) {
    Set<String> keys = new TreeSet<>(auditEventResponse.before().keySet());
    keys.addAll(auditEventResponse.proposed().keySet());
    keys.addAll(auditEventResponse.after().keySet());
    if (keys.isEmpty()) keys.add("");
    List<List<String>> result = new ArrayList<>();
    for (String fieldName : keys) {
      String before = auditEventResponse.before().getOrDefault(fieldName, "");
      String proposed = auditEventResponse.proposed().getOrDefault(fieldName, "");
      String after = auditEventResponse.after().getOrDefault(fieldName, "");
      if (!fieldName.isEmpty() && before.equals(proposed) && proposed.equals(after)) continue;
      result.add(
          Arrays.asList(
              auditEventResponse.id(),
              Integer.toString(auditEventResponse.schemaVersion()),
              auditEventResponse.eventType(),
              auditEventResponse.occurredAt().toString(),
              auditEventResponse.category(),
              auditEventResponse.feature(),
              auditEventResponse.scope(),
              auditEventResponse.recordId(),
              auditEventResponse.reference(),
              auditEventResponse.action(),
              auditEventResponse.status(),
              auditEventResponse.actor(),
              auditEventResponse.maker(),
              auditEventResponse.checker(),
              auditEventResponse.reason(),
              auditEventResponse.source(),
              auditEventResponse.endpoint(),
              auditEventResponse.httpMethod(),
              auditEventResponse.httpStatus() == null
                  ? ""
                  : auditEventResponse.httpStatus().toString(),
              auditEventResponse.outcome(),
              auditEventResponse.errorCode(),
              auditEventResponse.sourceIp(),
              auditEventResponse.userAgent(),
              auditEventResponse.riskLevel(),
              auditEventResponse.durationMs() == null
                  ? ""
                  : auditEventResponse.durationMs().toString(),
              auditEventResponse.metadata().toString(),
              fieldName,
              before,
              proposed,
              after));
    }
    if (result.isEmpty())
      result.add(
          Arrays.asList(
              auditEventResponse.id(),
              Integer.toString(auditEventResponse.schemaVersion()),
              auditEventResponse.eventType(),
              auditEventResponse.occurredAt().toString(),
              auditEventResponse.category(),
              auditEventResponse.feature(),
              auditEventResponse.scope(),
              auditEventResponse.recordId(),
              auditEventResponse.reference(),
              auditEventResponse.action(),
              auditEventResponse.status(),
              auditEventResponse.actor(),
              auditEventResponse.maker(),
              auditEventResponse.checker(),
              auditEventResponse.reason(),
              auditEventResponse.source(),
              auditEventResponse.endpoint(),
              auditEventResponse.httpMethod(),
              auditEventResponse.httpStatus() == null
                  ? ""
                  : auditEventResponse.httpStatus().toString(),
              auditEventResponse.outcome(),
              auditEventResponse.errorCode(),
              auditEventResponse.sourceIp(),
              auditEventResponse.userAgent(),
              auditEventResponse.riskLevel(),
              auditEventResponse.durationMs() == null
                  ? ""
                  : auditEventResponse.durationMs().toString(),
              auditEventResponse.metadata().toString(),
              "",
              "",
              "",
              ""));
    return result;
  }

  public static String safe(String value) {
    if (value == null) return "";
    String leading = value.stripLeading();
    return !leading.isEmpty() && "=+-@".indexOf(leading.charAt(0)) >= 0 ? "'" + value : value;
  }
}
