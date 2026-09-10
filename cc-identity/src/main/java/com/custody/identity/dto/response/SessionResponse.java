package com.custody.identity.dto.response;

import java.util.List;

public record SessionResponse(
    String user, String name, List<GroupResponse> groups, List<GrantResponse> grants) {}
