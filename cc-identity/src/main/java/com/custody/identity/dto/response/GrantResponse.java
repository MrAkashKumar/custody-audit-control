package com.custody.identity.dto.response;

public record GrantResponse(
    String id, String groupId, String feature, String action, String scope) {}
