package com.custody.core.service;

import com.custody.core.dto.AuditDraft;

public interface AuditWriter {
  void append(AuditDraft event);
}
