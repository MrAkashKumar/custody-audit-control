package com.custody.audit.service.impl;

import com.custody.audit.model.AuditEvent;
import com.custody.audit.repository.AuditRepository;
import com.custody.core.dto.AuditActivityDraft;
import com.custody.core.service.AuditActivityRecorder;
import com.custody.core.utils.Ids;
import com.custody.core.utils.JsonValues;
import java.time.Clock;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditActivityRecorderImpl implements AuditActivityRecorder {
  private final AuditRepository repository;
  private final JsonValues json;
  private final Clock clock;

  public AuditActivityRecorderImpl(
      AuditRepository auditRepository, JsonValues jsonValues, Clock auditClock) {
    this.repository = auditRepository;
    this.json = jsonValues;
    this.clock = auditClock;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(AuditActivityDraft activity) {
    String correlationId = traceId();
    AuditEvent auditEvent = new AuditEvent();
    auditEvent.setId(Ids.next());
    auditEvent.setSchemaVersion(2);
    auditEvent.setEventType(activity.eventType());
    auditEvent.setCategory(activity.category().name());
    auditEvent.setFeature(activity.feature());
    auditEvent.setScope(activity.scope());
    auditEvent.setRecordId(activity.subject().isBlank() ? activity.endpoint() : activity.subject());
    auditEvent.setReferenceId(correlationId);
    auditEvent.setAction(activity.eventType());
    auditEvent.setStatus(activity.outcome().name());
    auditEvent.setActor(activity.actor());
    auditEvent.setMaker(activity.actor());
    auditEvent.setChecker("");
    auditEvent.setReason(activity.reason());
    auditEvent.setSource("HTTP");
    auditEvent.setTraceId(correlationId);
    auditEvent.setEndpoint(activity.endpoint());
    auditEvent.setHttpMethod(activity.httpMethod());
    auditEvent.setHttpStatus(activity.httpStatus());
    auditEvent.setOutcome(activity.outcome().name());
    auditEvent.setErrorCode(activity.errorCode());
    auditEvent.setSourceIp(activity.sourceIp());
    auditEvent.setUserAgent(activity.userAgent());
    auditEvent.setRiskLevel(activity.riskLevel());
    auditEvent.setDurationMs(activity.durationMs());
    auditEvent.setMetadata(json.write(activity.metadata()));
    auditEvent.setOccurredAt(clock.instant());
    auditEvent.setBeforeValues(json.write(Map.of()));
    auditEvent.setProposedValues(json.write(Map.of()));
    auditEvent.setAfterValues(json.write(Map.of()));
    repository.saveAndFlush(auditEvent);
  }

  private String traceId() {
    String traceId = MDC.get("traceId");
    return traceId == null || traceId.isBlank() ? Ids.next() : traceId;
  }
}
