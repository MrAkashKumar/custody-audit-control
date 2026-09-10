package com.custody.workflow.model;

import com.custody.core.enums.OperationalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "cc_setup_record")
public class SetupRecord {
  @Id private String id;
  private String feature;
  private String scope;
  private String businessKey;

  @Column(length = 16000)
  private String data;

  @Enumerated(EnumType.STRING)
  private OperationalStatus status;

  private Instant updatedAt;
  @Version private long version;

  public SetupRecord() {}

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

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public OperationalStatus getStatus() {
    return status;
  }

  public void setStatus(OperationalStatus status) {
    this.status = status;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public long getVersion() {
    return version;
  }
}
