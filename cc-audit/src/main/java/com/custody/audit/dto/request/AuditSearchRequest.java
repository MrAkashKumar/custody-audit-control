package com.custody.audit.dto.request;

import java.time.LocalDate;

public record AuditSearchRequest(
    String category,
    String feature,
    String record,
    String field,
    String user,
    String action,
    String status,
    String reference,
    String screen,
    String scope,
    LocalDate from,
    LocalDate to) {
  public static AuditSearchRequest empty() {
    return new AuditSearchRequest(
        null, null, null, null, null, null, null, null, null, null, null, null);
  }
}
