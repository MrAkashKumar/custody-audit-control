package com.custody.app.audit;

import com.custody.core.dto.AuditActivityDraft;
import com.custody.core.enums.AuditCategory;
import com.custody.core.enums.AuditOutcome;
import com.custody.core.service.AuditActivityRecorder;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class ApiAuditPublisher {
  private static final Logger log = LoggerFactory.getLogger(ApiAuditPublisher.class);
  private final AuditActivityRecorder recorder;
  private final SuspiciousActivityDetector suspiciousActivityDetector;

  public ApiAuditPublisher(
      AuditActivityRecorder auditActivityRecorder, SuspiciousActivityDetector activityDetector) {
    this.recorder = auditActivityRecorder;
    this.suspiciousActivityDetector = activityDetector;
  }

  public void record(
      HttpServletRequest request,
      int status,
      String actor,
      String subject,
      String requestedEventType,
      long durationMs) {
    String safeActor = value(actor, "ANONYMOUS", 255);
    String safeSubject = value(subject, "", 255);
    String sourceIp = value(request.getRemoteAddr(), "unknown", 64);
    AuditRiskAssessment assessment =
        suspiciousActivityDetector.assess(requestedEventType, sourceIp + "|" + safeSubject, status);
    String errorCode = value(request.getAttribute(AuditRequestAttributes.ERROR_CODE), "", 255);
    String endpoint =
        value(
            request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE),
            request.getRequestURI(),
            1000);
    try {
      recorder.record(
          AuditActivityDraft.builder(AuditCategory.SECURITY, assessment.eventType())
              .actor(safeActor)
              .subject(safeSubject)
              .endpoint(endpoint)
              .httpMethod(value(request.getMethod(), "UNKNOWN", 16))
              .httpStatus(status)
              .outcome(status < 400 ? AuditOutcome.SUCCESS : AuditOutcome.FAILURE)
              .errorCode(errorCode)
              .sourceIp(sourceIp)
              .userAgent(value(request.getHeader("User-Agent"), "", 1000))
              .riskLevel(assessment.riskLevel())
              .reason(assessment.reason())
              .durationMs(Math.max(0, durationMs))
              .metadata(Map.of("requestPath", value(request.getRequestURI(), "", 1000)))
              .build());
    } catch (RuntimeException auditFailure) {
      log.error(
          "event=audit_activity_capture_failure endpoint={} status={}",
          endpoint,
          status,
          auditFailure);
    }
  }

  private String value(Object candidate, String fallback, int maximumLength) {
    String value = Objects.toString(candidate, fallback).replaceAll("[\\r\\n]", " ");
    return value.length() <= maximumLength ? value : value.substring(0, maximumLength);
  }
}
