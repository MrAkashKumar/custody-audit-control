package com.custody.core.builder;

import com.custody.core.dto.AuditDraft;
import com.custody.core.enums.AuditCategory;
import java.util.Map;

/** Named construction of evidence; build validates and freezes both snapshots. */
public final class AuditDraftBuilder {
  private final AuditCategory category;
  private String feature;
  private String scope;
  private String recordId;
  private String reference;
  private String action;
  private String status;
  private String actor;
  private String maker;
  private String checker = "";
  private String reason;
  private String source;
  private Map<String, String> before;
  private Map<String, String> proposed = Map.of();
  private Map<String, String> after;

  public AuditDraftBuilder(AuditCategory category) {
    this.category = category;
  }

  public AuditDraftBuilder feature(String feature) {
    this.feature = feature;
    return this;
  }

  public AuditDraftBuilder scope(String scope) {
    this.scope = scope;
    return this;
  }

  public AuditDraftBuilder recordId(String recordId) {
    this.recordId = recordId;
    return this;
  }

  public AuditDraftBuilder reference(String reference) {
    this.reference = reference;
    return this;
  }

  public AuditDraftBuilder action(String action) {
    this.action = action;
    return this;
  }

  public AuditDraftBuilder status(String status) {
    this.status = status;
    return this;
  }

  public AuditDraftBuilder actor(String actor) {
    this.actor = actor;
    return this;
  }

  public AuditDraftBuilder maker(String maker) {
    this.maker = maker;
    return this;
  }

  public AuditDraftBuilder checker(String checker) {
    this.checker = checker;
    return this;
  }

  public AuditDraftBuilder reason(String reason) {
    this.reason = reason;
    return this;
  }

  public AuditDraftBuilder source(String source) {
    this.source = source;
    return this;
  }

  public AuditDraftBuilder before(Map<String, String> before) {
    this.before = before;
    return this;
  }

  /** Values requested by a maker, separate from the values that actually became effective. */
  public AuditDraftBuilder proposed(Map<String, String> proposed) {
    this.proposed = proposed;
    return this;
  }

  public AuditDraftBuilder after(Map<String, String> after) {
    this.after = after;
    return this;
  }

  public AuditDraft build() {
    return new AuditDraft(
        category, feature, scope, recordId, reference, action, status, actor, maker, checker,
        reason, source, before, proposed, after);
  }
}
