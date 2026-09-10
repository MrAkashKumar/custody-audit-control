package com.custody.core.dto;

import com.custody.core.builder.AuditActivityDraftBuilder;
import com.custody.core.enums.AuditCategory;
import com.custody.core.enums.AuditOutcome;
import com.custody.core.error.CoreErrorCatalog;
import com.custody.core.exception.AppException;
import java.util.Map;

/** Trusted request/security evidence that may need to survive a failed business transaction. */
public record AuditActivityDraft(
    AuditCategory category,
    String eventType,
    String feature,
    String scope,
    String actor,
    String subject,
    String endpoint,
    String httpMethod,
    int httpStatus,
    AuditOutcome outcome,
    String errorCode,
    String sourceIp,
    String userAgent,
    String riskLevel,
    String reason,
    long durationMs,
    Map<String, String> metadata) {
  public AuditActivityDraft {
    AppException.require(category != null, CoreErrorCatalog.AUDIT_EVIDENCE_INVALID);
    for (String required :
        new String[] {eventType, feature, scope, actor, endpoint, httpMethod, riskLevel, reason})
      AppException.require(
          required != null && !required.isBlank(), CoreErrorCatalog.AUDIT_EVIDENCE_INVALID);
    AppException.require(outcome != null, CoreErrorCatalog.AUDIT_EVIDENCE_INVALID);
    for (String optional : new String[] {subject, errorCode, sourceIp, userAgent})
      AppException.require(optional != null, CoreErrorCatalog.AUDIT_EVIDENCE_INVALID);
    AppException.require(
        httpStatus >= 100 && httpStatus <= 599, CoreErrorCatalog.AUDIT_EVIDENCE_INVALID);
    AppException.require(durationMs >= 0, CoreErrorCatalog.AUDIT_EVIDENCE_INVALID);
    AppException.require(metadata != null, CoreErrorCatalog.AUDIT_EVIDENCE_INVALID);
    metadata = Map.copyOf(metadata);
  }

  public static AuditActivityDraftBuilder builder(AuditCategory category, String eventType) {
    return new AuditActivityDraftBuilder(category, eventType);
  }
}
