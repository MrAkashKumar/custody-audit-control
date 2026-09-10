package com.custody.identity.dto;

public record AuthenticationCredential(String username, String hash, boolean active) {}
