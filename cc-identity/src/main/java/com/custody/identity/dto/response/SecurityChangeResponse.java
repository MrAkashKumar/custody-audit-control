package com.custody.identity.dto.response;

import java.time.Instant;
import java.util.Map;

public record SecurityChangeResponse(
    String id,
    String kind,
    Map<String, String> values,
    String maker,
    String checker,
    String reason,
    String status,
    Instant submittedAt) {}
