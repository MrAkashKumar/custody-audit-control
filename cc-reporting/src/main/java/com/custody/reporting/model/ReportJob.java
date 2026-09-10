package com.custody.reporting.model;

import com.custody.reporting.enums.ReportFormat;
import com.custody.reporting.enums.ReportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "cc_report_job")
public class ReportJob {
  @Id private String id;
  private String owner;
  private String audienceGroup;
  private String triggerType;
  private String occurrenceKey;

  @Enumerated(EnumType.STRING)
  private ReportFormat format;

  @Enumerated(EnumType.STRING)
  private ReportStatus status;

  @Column(length = 8000)
  private String criteria;

  @Column(length = 16000)
  private String allowedPairs;

  @Column(length = 16000)
  private String fieldFootprint;

  private String traceId;
  private String failureCode;
  private Instant requestedAt;
  private Instant readyAt;
  private Instant claimedAt;
  private Instant expiresAt;
  private long rowCount;

  @Version private long version;

  public ReportJob() {}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getAudienceGroup() {
    return audienceGroup;
  }

  public void setAudienceGroup(String audienceGroup) {
    this.audienceGroup = audienceGroup;
  }

  public String getTriggerType() {
    return triggerType;
  }

  public void setTriggerType(String triggerType) {
    this.triggerType = triggerType;
  }

  public String getOccurrenceKey() {
    return occurrenceKey;
  }

  public void setOccurrenceKey(String occurrenceKey) {
    this.occurrenceKey = occurrenceKey;
  }

  public ReportFormat getFormat() {
    return format;
  }

  public void setFormat(ReportFormat format) {
    this.format = format;
  }

  public ReportStatus getStatus() {
    return status;
  }

  public void setStatus(ReportStatus status) {
    this.status = status;
  }

  public String getCriteria() {
    return criteria;
  }

  public void setCriteria(String criteria) {
    this.criteria = criteria;
  }

  public String getAllowedPairs() {
    return allowedPairs;
  }

  public void setAllowedPairs(String allowedPairs) {
    this.allowedPairs = allowedPairs;
  }

  public String getFieldFootprint() {
    return fieldFootprint;
  }

  public void setFieldFootprint(String fieldFootprint) {
    this.fieldFootprint = fieldFootprint;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public String getFailureCode() {
    return failureCode;
  }

  public void setFailureCode(String failureCode) {
    this.failureCode = failureCode;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public void setRequestedAt(Instant requestedAt) {
    this.requestedAt = requestedAt;
  }

  public Instant getReadyAt() {
    return readyAt;
  }

  public void setReadyAt(Instant readyAt) {
    this.readyAt = readyAt;
  }

  public Instant getClaimedAt() {
    return claimedAt;
  }

  public void setClaimedAt(Instant claimedAt) {
    this.claimedAt = claimedAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public long getRowCount() {
    return rowCount;
  }

  public void setRowCount(long rowCount) {
    this.rowCount = rowCount;
  }

  public long getVersion() {
    return version;
  }
}
