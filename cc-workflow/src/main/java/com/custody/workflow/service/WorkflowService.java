package com.custody.workflow.service;

import com.custody.workflow.dto.request.ApprovalDecisionRequest;
import com.custody.workflow.dto.request.ChangeSetupStatusRequest;
import com.custody.workflow.dto.request.CreateSetupRequest;
import com.custody.workflow.dto.request.UpdateSetupRequest;
import com.custody.workflow.dto.response.ChangeRequestResponse;
import com.custody.workflow.dto.response.SetupRecordResponse;
import java.util.List;

/** Public use-case boundary for the WorkflowService capability. */
public interface WorkflowService {
  List<SetupRecordResponse> records(String actor, String feature);

  List<ChangeRequestResponse> pending(String actor);

  ChangeRequestResponse add(String actor, String feature, CreateSetupRequest input, String source);

  ChangeRequestResponse edit(String actor, String id, UpdateSetupRequest input);

  ChangeRequestResponse status(String actor, String id, ChangeSetupStatusRequest input);

  ChangeRequestResponse decide(String actor, String id, ApprovalDecisionRequest input);
}
