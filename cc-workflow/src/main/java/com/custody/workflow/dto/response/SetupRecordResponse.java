package com.custody.workflow.dto.response;

import com.custody.core.enums.OperationalStatus;
import java.time.Instant;
import java.util.Map;

public record SetupRecordResponse(
    String id,
    String feature,
    String scope,
    String businessKey,
    Map<String, String> values,
    OperationalStatus status,
    long version,
    Instant updatedAt) {}
