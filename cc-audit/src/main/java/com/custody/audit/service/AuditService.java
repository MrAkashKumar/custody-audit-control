package com.custody.audit.service;

import com.custody.audit.dto.request.AuditSearchRequest;
import com.custody.audit.dto.response.AuditEventResponse;
import com.custody.audit.dto.response.AuditPageResponse;
import com.custody.core.enums.Action;
import java.time.Instant;

/** Searches immutable evidence using current feature, scope and field permissions. */
public interface AuditService {
  AuditPageResponse search(String actor, AuditSearchRequest auditSearchRequest, int page, int size);

  /** Reads a permission-controlled page at a fixed cutoff for consistent report generation. */
  AuditPageResponse query(
      String actor,
      AuditSearchRequest auditSearchRequest,
      int page,
      int size,
      Instant cutoff,
      Action permission);

  AuditEventResponse detail(String actor, String id);
}
