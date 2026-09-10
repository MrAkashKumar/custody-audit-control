package com.custody.identity.dto.response;

import java.util.List;

public record DirectoryResponse(
    List<UserResponse> users,
    List<GroupResponse> groups,
    List<GrantResponse> grants,
    List<SecurityChangeResponse> changes) {}
