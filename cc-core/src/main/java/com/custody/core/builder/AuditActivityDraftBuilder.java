package com.custody.core.builder;

import com.custody.core.dto.AuditActivityDraft;
import com.custody.core.enums.AuditCategory;
import com.custody.core.enums.AuditOutcome;
import java.util.Map;

/** Named construction prevents positional mistakes in security and HTTP evidence. */
public final class AuditActivityDraftBuilder {
  private final AuditCategory category;
  private final String eventType;
  private String feature = "administration";
  private String scope = "SYSTEM";
  private String actor = "ANONYMOUS";
  private String subject = "";
  private String endpoint;
  private String httpMethod;
  private int httpStatus;
  private AuditOutcome outcome;
  private String errorCode = "";
  private String sourceIp = "";
  private String userAgent = "";
  private String riskLevel = "LOW";
  private String reason;
  private long durationMs;
  private Map<String, String> metadata = Map.of();

  public AuditActivityDraftBuilder(AuditCategory category, String eventType) {
    this.category = category;
    this.eventType = eventType;
  }

  public AuditActivityDraftBuilder feature(String feature) {
    this.feature = feature;
    return this;
  }

  public AuditActivityDraftBuilder scope(String scope) {
    this.scope = scope;
    return this;
  }

  public AuditActivityDraftBuilder actor(String actor) {
    this.actor = actor;
    return this;
  }

  public AuditActivityDraftBuilder subject(String subject) {
    this.subject = subject;
    return this;
  }

  public AuditActivityDraftBuilder endpoint(String endpoint) {
    this.endpoint = endpoint;
    return this;
  }

  public AuditActivityDraftBuilder httpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
    return this;
  }

  public AuditActivityDraftBuilder httpStatus(int httpStatus) {
    this.httpStatus = httpStatus;
    return this;
  }

  public AuditActivityDraftBuilder outcome(AuditOutcome outcome) {
    this.outcome = outcome;
    return this;
  }

  public AuditActivityDraftBuilder errorCode(String errorCode) {
    this.errorCode = errorCode;
    return this;
  }

  public AuditActivityDraftBuilder sourceIp(String sourceIp) {
    this.sourceIp = sourceIp;
    return this;
  }

  public AuditActivityDraftBuilder userAgent(String userAgent) {
    this.userAgent = userAgent;
    return this;
  }

  public AuditActivityDraftBuilder riskLevel(String riskLevel) {
    this.riskLevel = riskLevel;
    return this;
  }

  public AuditActivityDraftBuilder reason(String reason) {
    this.reason = reason;
    return this;
  }

  public AuditActivityDraftBuilder durationMs(long durationMs) {
    this.durationMs = durationMs;
    return this;
  }

  public AuditActivityDraftBuilder metadata(Map<String, String> metadata) {
    this.metadata = metadata;
    return this;
  }

  public AuditActivityDraft build() {
    return new AuditActivityDraft(
        category,
        eventType,
        feature,
        scope,
        actor,
        subject,
        endpoint,
        httpMethod,
        httpStatus,
        outcome,
        errorCode,
        sourceIp,
        userAgent,
        riskLevel,
        reason,
        durationMs,
        metadata);
  }
}
