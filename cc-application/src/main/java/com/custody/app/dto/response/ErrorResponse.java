package com.custody.app.dto.response;

import java.util.Map;

public record ErrorResponse(
    int status, String code, String message, String traceId, Map<String, String> fieldErrors) {
  public ErrorResponse {
    fieldErrors = fieldErrors == null ? Map.of() : Map.copyOf(fieldErrors);
  }
}
