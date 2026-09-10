package com.custody.core.audit.spi;

import com.custody.core.dto.AuditDraft;

/** Feature-owned mapping of a trusted business event to persistent evidence. */
public interface AuditEventAdapter<T> {
  Class<T> eventType();

  AuditDraft toAuditDraft(T event);
}
