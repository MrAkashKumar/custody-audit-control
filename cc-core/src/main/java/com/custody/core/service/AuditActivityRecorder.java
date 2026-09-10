package com.custody.core.service;

import com.custody.core.dto.AuditActivityDraft;

/** Independent evidence path for failed requests and security activity. */
public interface AuditActivityRecorder {
  void record(AuditActivityDraft activity);
}
