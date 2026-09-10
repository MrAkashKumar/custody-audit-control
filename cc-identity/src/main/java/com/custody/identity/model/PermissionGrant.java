package com.custody.identity.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "cc_permission_grant")
public class PermissionGrant {
  @Id private String id;
  private String groupId;
  private String feature;
  private String action;
  private String scope;
  private String hiddenFields;
  @Version private long version;

  public PermissionGrant() {}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getFeature() {
    return feature;
  }

  public void setFeature(String feature) {
    this.feature = feature;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public String getHiddenFields() {
    return hiddenFields;
  }

  public void setHiddenFields(String hiddenFields) {
    this.hiddenFields = hiddenFields;
  }

  public long getVersion() {
    return version;
  }
}
