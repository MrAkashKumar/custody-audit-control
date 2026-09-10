package com.custody.app.controller;

import com.custody.app.constants.ApiRoutes;
import com.custody.identity.dto.request.SecurityChangeRequest;
import com.custody.identity.dto.response.DirectoryResponse;
import com.custody.identity.dto.response.SecurityChangeResponse;
import com.custody.identity.service.IdentityService;
import com.custody.workflow.dto.request.ApprovalDecisionRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AdministrationController {
  private final IdentityService identity;

  AdministrationController(IdentityService identityService) {
    this.identity = identityService;
  }

  @GetMapping(ApiRoutes.ADMIN)
  DirectoryResponse directory(Principal principal) {
    return identity.directory(principal.getName());
  }

  @PostMapping(ApiRoutes.ADMIN_CHANGES)
  SecurityChangeResponse change(
      Principal principal, @Valid @RequestBody SecurityChangeRequest securityChangeRequest) {
    return identity.propose(principal.getName(), securityChangeRequest);
  }

  @PostMapping(ApiRoutes.ADMIN_CHANGE_DECISION)
  SecurityChangeResponse decide(
      Principal principal,
      @PathVariable String id,
      @Valid @RequestBody ApprovalDecisionRequest approvalDecisionRequest) {
    return identity.decide(
        principal.getName(),
        id,
        approvalDecisionRequest.approve(),
        approvalDecisionRequest.reason());
  }
}
