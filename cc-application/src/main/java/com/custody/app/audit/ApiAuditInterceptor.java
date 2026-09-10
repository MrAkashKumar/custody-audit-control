package com.custody.app.audit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiAuditInterceptor implements HandlerInterceptor {
  private final ApiAuditPublisher publisher;

  public ApiAuditInterceptor(ApiAuditPublisher apiAuditPublisher) {
    this.publisher = apiAuditPublisher;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    request.setAttribute(AuditRequestAttributes.STARTED_NANOS, System.nanoTime());
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      Exception exception) {
    Object started = request.getAttribute(AuditRequestAttributes.STARTED_NANOS);
    long durationMs =
        started instanceof Long startedNanos ? (System.nanoTime() - startedNanos) / 1_000_000 : 0;
    Principal principal = request.getUserPrincipal();
    publisher.record(
        request,
        response.getStatus(),
        principal == null ? "ANONYMOUS" : principal.getName(),
        "",
        "API_REQUEST",
        durationMs);
  }
}
