package com.custody.audit.dto.response;

import java.util.List;

public record AuditPageResponse(
    List<AuditEventResponse> content, long totalElements, int number, int size) {}
