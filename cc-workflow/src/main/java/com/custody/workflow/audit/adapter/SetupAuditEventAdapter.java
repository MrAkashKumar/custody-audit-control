package com.custody.workflow.audit.adapter;

import com.custody.core.audit.spi.AuditEventAdapter;
import com.custody.core.dto.AuditDraft;
import com.custody.core.enums.AuditCategory;
import com.custody.core.enums.RequestStatus;
import com.custody.workflow.audit.event.SetupAuditEvent;
import org.springframework.stereotype.Component;

/** One adapter serves every registered feature using the maker-checker setup workflow. */
@Component
public class SetupAuditEventAdapter implements AuditEventAdapter<SetupAuditEvent> {
  @Override
  public Class<SetupAuditEvent> eventType() {
    return SetupAuditEvent.class;
  }

  @Override
  public AuditDraft toAuditDraft(SetupAuditEvent event) {
    var change = event.change();
    boolean pending = change.status() == RequestStatus.PENDING;
    boolean approved = change.status() == RequestStatus.APPROVED;
    return AuditDraft.builder(AuditCategory.STATIC)
        .feature(change.feature())
        .scope(change.scope())
        .recordId(change.businessKey())
        .reference(change.id())
        .action(change.action().name())
        .status(change.status().name())
        .actor(event.actor())
        .maker(change.maker())
        .checker(pending ? "" : change.checker())
        .reason(pending ? change.reason() : change.decisionReason())
        .source(event.source())
        .before(change.before())
        .proposed(change.proposed())
        .after(approved ? event.resultingValues() : change.before())
        .build();
  }
}
