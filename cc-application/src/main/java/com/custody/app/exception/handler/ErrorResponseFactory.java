package com.custody.app.exception.handler;

import com.custody.app.audit.AuditRequestAttributes;
import com.custody.app.dto.response.ErrorResponse;
import com.custody.core.error.ErrorDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public final class ErrorResponseFactory {
  private static final String DEFAULT_SAFE_MESSAGE =
      "We could not complete the request. Contact support with the trace reference.";
  private static final String TRACE_ID_KEY = "traceId";
  private static final String UNAVAILABLE_TRACE_ID = "unavailable";
  private static final String NO_STORE = "no-store";

  private final MessageSource messages;
  private final ObjectMapper mapper;
  private final HttpServletRequest request;

  public ErrorResponseFactory(
      MessageSource messageSource, ObjectMapper objectMapper, HttpServletRequest httpRequest) {
    this.messages = messageSource;
    this.mapper = objectMapper;
    this.request = httpRequest;
  }

  public ErrorResponse problem(ErrorDefinition error, Map<String, String> fields) {
    request.setAttribute(AuditRequestAttributes.ERROR_CODE, error.code());
    return new ErrorResponse(
        error.status(),
        error.code(),
        messages.getMessage(error.messageKey(), null, DEFAULT_SAFE_MESSAGE, Locale.ENGLISH),
        Objects.toString(MDC.get(TRACE_ID_KEY), UNAVAILABLE_TRACE_ID),
        fields);
  }

  public ResponseEntity<ErrorResponse> response(
      ErrorDefinition error, Map<String, String> fieldErrors) {
    return ResponseEntity.status(error.status())
        .cacheControl(CacheControl.noStore())
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem(error, fieldErrors));
  }

  public void write(HttpServletResponse response, ErrorDefinition error) throws IOException {
    if (response.isCommitted()) {
      return;
    }
    response.setStatus(error.status());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setHeader("Cache-Control", NO_STORE);
    mapper.writeValue(response.getOutputStream(), problem(error, Map.of()));
  }
}
