package com.custody.workflow.model;

import com.custody.core.enums.ChangeAction;
import com.custody.core.enums.RequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "cc_change_request")
public class ChangeRequest {
  @Id private String id;
  private String feature;
  private String scope;
  private String businessKey;
  private String recordId;
  private String pendingKey;

  @Enumerated(EnumType.STRING)
  private ChangeAction action;

  @Enumerated(EnumType.STRING)
  private RequestStatus status;

  private long baselineVersion;

  @Column(length = 16000)
  private String beforeData;

  @Column(length = 16000)
  private String proposedData;

  private String maker;
  private String checker;

  @Column(length = 1000)
  private String reason;

  @Column(length = 1000)
  private String decisionReason;

  private Instant submittedAt;
  private Instant decidedAt;
  @Version private long version;

  public ChangeRequest() {}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getFeature() {
    return feature;
  }

  public void setFeature(String feature) {
    this.feature = feature;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public String getBusinessKey() {
    return businessKey;
  }

  public void setBusinessKey(String businessKey) {
    this.businessKey = businessKey;
  }

  public String getRecordId() {
    return recordId;
  }

  public void setRecordId(String recordId) {
    this.recordId = recordId;
  }

  public String getPendingKey() {
    return pendingKey;
  }

  public void setPendingKey(String pendingKey) {
    this.pendingKey = pendingKey;
  }

  public ChangeAction getAction() {
    return action;
  }

  public void setAction(ChangeAction action) {
    this.action = action;
  }

  public RequestStatus getStatus() {
    return status;
  }

  public void setStatus(RequestStatus status) {
    this.status = status;
  }

  public long getBaselineVersion() {
    return baselineVersion;
  }

  public void setBaselineVersion(long baselineVersion) {
    this.baselineVersion = baselineVersion;
  }

  public String getBeforeData() {
    return beforeData;
  }

  public void setBeforeData(String beforeData) {
    this.beforeData = beforeData;
  }

  public String getProposedData() {
    return proposedData;
  }

  public void setProposedData(String proposedData) {
    this.proposedData = proposedData;
  }

  public String getMaker() {
    return maker;
  }

  public void setMaker(String maker) {
    this.maker = maker;
  }

  public String getChecker() {
    return checker;
  }

  public void setChecker(String checker) {
    this.checker = checker;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getDecisionReason() {
    return decisionReason;
  }

  public void setDecisionReason(String decisionReason) {
    this.decisionReason = decisionReason;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(Instant submittedAt) {
    this.submittedAt = submittedAt;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public void setDecidedAt(Instant decidedAt) {
    this.decidedAt = decidedAt;
  }

  public long getVersion() {
    return version;
  }
}
