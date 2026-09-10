package com.custody.identity.service;

import com.custody.identity.dto.AuthenticationCredential;
import com.custody.identity.dto.request.SecurityChangeRequest;
import com.custody.identity.dto.response.DirectoryResponse;
import com.custody.identity.dto.response.SecurityChangeResponse;
import com.custody.identity.dto.response.SessionResponse;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Public use-case boundary for the IdentityService capability. */
public interface IdentityService {
  Optional<AuthenticationCredential> credential(String name);

  SessionResponse session(String actor);

  DirectoryResponse directory(String actor);

  SecurityChangeResponse propose(String actor, SecurityChangeRequest input);

  SecurityChangeResponse decide(String actor, String id, boolean approve, String reason);

  void seed(String password, PasswordEncoder encoder);
}
