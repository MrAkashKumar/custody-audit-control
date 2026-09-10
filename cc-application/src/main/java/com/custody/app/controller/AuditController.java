package com.custody.app.controller;

import com.custody.app.constants.ApiRoutes;
import com.custody.audit.dto.request.AuditSearchRequest;
import com.custody.audit.dto.response.AuditEventResponse;
import com.custody.audit.dto.response.AuditPageResponse;
import com.custody.audit.service.AuditService;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AuditController {
  private final AuditService audit;

  AuditController(AuditService auditService) {
    this.audit = auditService;
  }

  @GetMapping(ApiRoutes.AUDIT)
  AuditPageResponse search(
      Principal principal,
      @ModelAttribute AuditSearchRequest filter,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return audit.search(principal.getName(), filter, page, size);
  }

  @GetMapping(ApiRoutes.AUDIT_DETAIL)
  AuditEventResponse detail(Principal principal, @PathVariable String id) {
    return audit.detail(principal.getName(), id);
  }
}
