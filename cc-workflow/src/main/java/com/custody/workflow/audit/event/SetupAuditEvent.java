package com.custody.workflow.audit.event;

import com.custody.workflow.dto.response.ChangeRequestResponse;
import java.util.Map;
import java.util.Objects;

/** A synchronous workflow fact; resulting values may differ from the original proposal. */
public record SetupAuditEvent(
    ChangeRequestResponse change,
    String actor,
    String source,
    Map<String, String> resultingValues) {
  public SetupAuditEvent {
    Objects.requireNonNull(change, "Change response is required");
    resultingValues = Map.copyOf(resultingValues);
  }
}
