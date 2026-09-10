package com.custody.audit.service.impl;

import com.custody.core.audit.spi.AuditEventAdapter;
import com.custody.core.dto.AuditDraft;
import com.custody.core.error.CoreErrorCatalog;
import com.custody.core.exception.AppException;
import com.custody.core.service.AuditCapture;
import com.custody.core.service.AuditWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Immutable adapter registry: no feature-specific dependencies or dispatch branches. */
@Service
public class AuditCaptureImpl implements AuditCapture {
  private final Map<Class<?>, Function<Object, AuditDraft>> adapters;
  private final AuditWriter auditWriter;

  public AuditCaptureImpl(List<AuditEventAdapter<?>> eventAdapters, AuditWriter auditWriter) {
    this.auditWriter = auditWriter;
    Map<Class<?>, Function<Object, AuditDraft>> registeredAdapters = new HashMap<>();
    for (AuditEventAdapter<?> adapter : eventAdapters) {
      Class<?> eventType =
          Objects.requireNonNull(adapter.eventType(), "Audit event type is required");
      if (registeredAdapters.putIfAbsent(eventType, bind(adapter, eventType)) != null) {
        throw new IllegalStateException("Duplicate audit adapter for " + eventType.getName());
      }
    }
    this.adapters = Map.copyOf(registeredAdapters);
  }

  private static <T> Function<Object, AuditDraft> bind(
      AuditEventAdapter<T> adapter, Class<?> eventType) {
    Class<T> declaredType = adapter.eventType();
    if (!declaredType.equals(eventType)) {
      throw new IllegalStateException("Audit adapter event type must be stable");
    }
    return event -> adapter.toAuditDraft(declaredType.cast(event));
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public <T> void record(T event) {
    AppException.require(event != null, CoreErrorCatalog.AUDIT_EVIDENCE_INVALID);
    Function<Object, AuditDraft> adapter = adapters.get(event.getClass());
    AppException.require(adapter != null, CoreErrorCatalog.AUDIT_EVIDENCE_INVALID);
    AuditDraft draft = adapter.apply(event);
    AppException.require(draft != null, CoreErrorCatalog.AUDIT_EVIDENCE_INVALID);
    auditWriter.append(draft);
  }
}
