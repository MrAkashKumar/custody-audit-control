package com.custody.app.controller;

import com.custody.app.constants.ApiRoutes;
import com.custody.workflow.dto.request.ApprovalDecisionRequest;
import com.custody.workflow.dto.request.ChangeSetupStatusRequest;
import com.custody.workflow.dto.request.CreateSetupRequest;
import com.custody.workflow.dto.request.UpdateSetupRequest;
import com.custody.workflow.dto.response.ChangeRequestResponse;
import com.custody.workflow.dto.response.SetupRecordResponse;
import com.custody.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class WorkflowController {
  private final WorkflowService workflow;

  WorkflowController(WorkflowService workflowService) {
    this.workflow = workflowService;
  }

  @GetMapping(ApiRoutes.FEATURE_RECORDS)
  List<SetupRecordResponse> records(Principal principal, @PathVariable String feature) {
    return workflow.records(principal.getName(), feature);
  }

  @PostMapping(ApiRoutes.FEATURE_RECORDS)
  @ResponseStatus(org.springframework.http.HttpStatus.ACCEPTED)
  ChangeRequestResponse add(
      Principal principal,
      @PathVariable String feature,
      @Valid @RequestBody CreateSetupRequest input) {
    return workflow.add(principal.getName(), feature, input, "Setup screen");
  }

  @PostMapping(ApiRoutes.RECORD_EDIT)
  ChangeRequestResponse edit(
      Principal principal, @PathVariable String id, @Valid @RequestBody UpdateSetupRequest input) {
    return workflow.edit(principal.getName(), id, input);
  }

  @PostMapping(ApiRoutes.RECORD_STATUS)
  ChangeRequestResponse status(
      Principal principal,
      @PathVariable String id,
      @Valid @RequestBody ChangeSetupStatusRequest input) {
    return workflow.status(principal.getName(), id, input);
  }

  @GetMapping(ApiRoutes.CHANGES)
  List<ChangeRequestResponse> changes(Principal principal) {
    return workflow.pending(principal.getName());
  }

  @PostMapping(ApiRoutes.CHANGE_DECISION)
  ChangeRequestResponse decide(
      Principal principal,
      @PathVariable String id,
      @Valid @RequestBody ApprovalDecisionRequest input) {
    return workflow.decide(principal.getName(), id, input);
  }
}
