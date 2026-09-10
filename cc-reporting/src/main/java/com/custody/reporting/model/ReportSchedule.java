package com.custody.reporting.model;

import com.custody.reporting.enums.ReportFormat;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "cc_report_schedule")
public class ReportSchedule {
  @Id private String id;
  private String owner;
  private String audienceGroup;
  private String name;
  private String category;
  private String feature;
  private String scope;

  @Enumerated(EnumType.STRING)
  private ReportFormat format;

  private boolean enabled;
  private int intervalDays;
  private Instant nextRunAt;
  private String lastFailure;
  @Version private long version;

  public ReportSchedule() {}

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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
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

  public ReportFormat getFormat() {
    return format;
  }

  public void setFormat(ReportFormat format) {
    this.format = format;
  }

  public boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getIntervalDays() {
    return intervalDays;
  }

  public void setIntervalDays(int intervalDays) {
    this.intervalDays = intervalDays;
  }

  public Instant getNextRunAt() {
    return nextRunAt;
  }

  public void setNextRunAt(Instant nextRunAt) {
    this.nextRunAt = nextRunAt;
  }

  public String getLastFailure() {
    return lastFailure;
  }

  public void setLastFailure(String lastFailure) {
    this.lastFailure = lastFailure;
  }

  public long getVersion() {
    return version;
  }
}
