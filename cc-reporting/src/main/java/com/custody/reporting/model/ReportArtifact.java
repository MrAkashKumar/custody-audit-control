package com.custody.reporting.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "cc_report_artifact")
public class ReportArtifact {
  @Id private String id;
  @Lob private byte[] content;

  public ReportArtifact() {}

  public ReportArtifact(String id, byte[] content) {
    this.id = id;
    this.content = content;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public byte[] getContent() {
    return content;
  }

  public void setContent(byte[] content) {
    this.content = content;
  }
}
