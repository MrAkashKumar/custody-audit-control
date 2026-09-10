package com.custody.core.dto;

import com.custody.core.builder.AuditDraftBuilder;
import com.custody.core.enums.AuditCategory;
import com.custody.core.error.CoreErrorCatalog;
import com.custody.core.exception.AppException;
import java.util.Map;

public record AuditDraft(
    AuditCategory category,
    String feature,
    String scope,
    String recordId,
    String reference,
    String action,
    String status,
    String actor,
    String maker,
    String checker,
    String reason,
    String source,
    Map<String, String> before,
    Map<String, String> proposed,
    Map<String, String> after) {
  public AuditDraft {
    AppException.require(category != null, CoreErrorCatalog.AUDIT_EVIDENCE_INVALID);
    for (String required :
        new String[] {
          feature, scope, recordId, reference, action, status, actor, maker, reason, source
        }) {
      AppException.require(
          required != null && !required.isBlank(), CoreErrorCatalog.AUDIT_EVIDENCE_INVALID);
    }
    AppException.require(checker != null, CoreErrorCatalog.AUDIT_EVIDENCE_INVALID);
    requireSnapshot(before);
    requireSnapshot(proposed);
    requireSnapshot(after);
    before = Map.copyOf(before);
    proposed = Map.copyOf(proposed);
    after = Map.copyOf(after);
  }

  public static AuditDraftBuilder builder(AuditCategory category) {
    return new AuditDraftBuilder(category);
  }

  private static void requireSnapshot(Map<String, String> snapshot) {
    AppException.require(snapshot != null, CoreErrorCatalog.AUDIT_EVIDENCE_INVALID);
    snapshot.forEach(
        (field, value) ->
            AppException.require(
                field != null && !field.isBlank() && value != null,
                CoreErrorCatalog.AUDIT_EVIDENCE_INVALID));
  }
}
