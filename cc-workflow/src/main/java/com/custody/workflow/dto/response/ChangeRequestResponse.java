package com.custody.workflow.dto.response;

import com.custody.core.enums.ChangeAction;
import com.custody.core.enums.RequestStatus;
import java.time.Instant;
import java.util.Map;

public record ChangeRequestResponse(
    String id,
    String feature,
    String scope,
    String businessKey,
    String recordId,
    ChangeAction action,
    RequestStatus status,
    Map<String, String> before,
    Map<String, String> proposed,
    String maker,
    String checker,
    String reason,
    String decisionReason,
    Instant submittedAt,
    Instant decidedAt) {
  public ChangeRequestResponse {
    before = Map.copyOf(before);
    proposed = Map.copyOf(proposed);
  }
}
