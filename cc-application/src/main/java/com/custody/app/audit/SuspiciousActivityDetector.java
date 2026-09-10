package com.custody.app.audit;

import com.custody.app.config.properties.SuspiciousActivityProperties;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Local signal generation; production deployments should also correlate events in their SIEM. */
@Component
class SuspiciousActivityDetector {
  private final SuspiciousActivityProperties properties;
  private final Clock clock;
  private final ConcurrentHashMap<String, Deque<Long>> failedLogins = new ConcurrentHashMap<>();

  SuspiciousActivityDetector(SuspiciousActivityProperties settings, Clock auditClock) {
    this.properties = settings;
    this.clock = auditClock;
  }

  AuditRiskAssessment assess(String requestedEventType, String subjectKey, int httpStatus) {
    if (requestedEventType.equals("AUTHENTICATION_FAILURE") && repeatedFailure(subjectKey))
      return new AuditRiskAssessment(
          "SUSPICIOUS_ACTIVITY", "HIGH", "Repeated authentication failures detected");
    if (requestedEventType.equals("AUTHENTICATION_FAILURE"))
      return new AuditRiskAssessment(requestedEventType, "MEDIUM", "Authentication failed");
    if (httpStatus == 403)
      return new AuditRiskAssessment("ACCESS_DENIED", "MEDIUM", "Authorisation was denied");
    if (httpStatus >= 500)
      return new AuditRiskAssessment("APPLICATION_ERROR", "HIGH", "Request failed internally");
    if (httpStatus >= 400)
      return new AuditRiskAssessment(
          "VALIDATION_FAILURE", "LOW", "Request validation or processing failed");
    return new AuditRiskAssessment(requestedEventType, "LOW", "Request completed");
  }

  private boolean repeatedFailure(String subjectKey) {
    if (failedLogins.size() >= properties.maximumTrackedSubjects()
        && !failedLogins.containsKey(subjectKey)) failedLogins.clear();
    long cutoff = clock.millis() - properties.windowSeconds() * 1000L;
    Deque<Long> attempts = failedLogins.computeIfAbsent(subjectKey, ignored -> new ArrayDeque<>());
    synchronized (attempts) {
      while (!attempts.isEmpty() && attempts.peekFirst() < cutoff) attempts.removeFirst();
      attempts.addLast(clock.millis());
      return attempts.size() >= properties.failedLoginThreshold();
    }
  }
}
