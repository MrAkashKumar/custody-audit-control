package com.custody.audit.dto.response;

import java.time.Instant;
import java.util.Map;

public record AuditEventResponse(
    String id,
    int schemaVersion,
    String eventType,
    String category,
    String feature,
    String scope,
    String recordId,
    String reference,
    String action,
    String status,
    String actor,
    String maker,
    String checker,
    String reason,
    String source,
    String traceId,
    String endpoint,
    String httpMethod,
    Integer httpStatus,
    String outcome,
    String errorCode,
    String sourceIp,
    String userAgent,
    String riskLevel,
    Long durationMs,
    Map<String, String> metadata,
    Instant occurredAt,
    Map<String, String> before,
    Map<String, String> proposed,
    Map<String, String> after) {}
