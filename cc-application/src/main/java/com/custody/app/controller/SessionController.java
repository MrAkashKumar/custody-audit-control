package com.custody.app.controller;

import com.custody.app.constants.ApiRoutes;
import com.custody.core.enums.Action;
import com.custody.identity.dto.response.SessionResponse;
import com.custody.identity.service.AccessPolicy;
import com.custody.identity.service.IdentityService;
import com.custody.workflow.model.FeatureDefinition;
import com.custody.workflow.service.FeatureRegistry;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SessionController {
  private final IdentityService identity;
  private final FeatureRegistry features;
  private final AccessPolicy access;

  SessionController(
      IdentityService identityService, FeatureRegistry featureRegistry, AccessPolicy accessPolicy) {
    this.identity = identityService;
    this.features = featureRegistry;
    this.access = accessPolicy;
  }

  @GetMapping(ApiRoutes.CSRF)
  Map<String, String> csrf(CsrfToken token) {
    return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
  }

  @GetMapping(ApiRoutes.SESSION)
  SessionResponse session(Principal principal) {
    return identity.session(principal.getName());
  }

  @GetMapping(ApiRoutes.FEATURES)
  List<FeatureDefinition> features(Principal principal) {
    return features.all().stream()
        .filter(
            featureDefinition ->
                !access.scopes(principal.getName(), featureDefinition.key(), Action.VIEW).isEmpty())
        .toList();
  }
}
