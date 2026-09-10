package com.custody.audit.service.impl;

import com.custody.audit.model.AuditEvent;
import com.custody.audit.repository.AuditRepository;
import com.custody.core.dto.AuditDraft;
import com.custody.core.service.AuditWriter;
import com.custody.core.utils.Ids;
import com.custody.core.utils.JsonValues;
import java.time.Clock;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Persists evidence in the caller's transaction; failure must roll back the business change. */
@Service
public class AuditWriterImpl implements AuditWriter {
  private final AuditRepository repository;
  private final JsonValues json;
  private final Clock clock;

  public AuditWriterImpl(AuditRepository repository, JsonValues json, Clock clock) {
    this.repository = repository;
    this.json = json;
    this.clock = clock;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  @Override
  public void append(AuditDraft draft) {
    AuditEvent auditEvent = new AuditEvent();
    auditEvent.setId(Ids.next());
    auditEvent.setSchemaVersion(2);
    auditEvent.setEventType("BUSINESS_CHANGE");
    auditEvent.setCategory(draft.category().name());
    auditEvent.setFeature(draft.feature());
    auditEvent.setScope(draft.scope());
    auditEvent.setRecordId(draft.recordId());
    auditEvent.setReferenceId(draft.reference());
    auditEvent.setAction(draft.action());
    auditEvent.setStatus(draft.status());
    auditEvent.setActor(draft.actor());
    auditEvent.setMaker(draft.maker());
    auditEvent.setChecker(draft.checker());
    auditEvent.setReason(draft.reason());
    auditEvent.setSource(draft.source());
    auditEvent.setTraceId(correlationId());
    auditEvent.setMetadata(json.write(java.util.Map.of()));
    auditEvent.setOccurredAt(clock.instant());
    auditEvent.setBeforeValues(json.write(draft.before()));
    auditEvent.setProposedValues(json.write(draft.proposed()));
    auditEvent.setAfterValues(json.write(draft.after()));
    repository.saveAndFlush(auditEvent);
  }

  private String correlationId() {
    String traceId = MDC.get("traceId");
    return traceId == null || traceId.isBlank() ? Ids.next() : traceId;
  }
}
