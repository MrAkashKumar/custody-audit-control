package com.custody.identity.dto.response;

import java.util.List;

public record UserResponse(String id, String name, boolean active, List<String> groups) {}
