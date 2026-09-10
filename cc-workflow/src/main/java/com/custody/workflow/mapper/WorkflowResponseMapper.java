package com.custody.workflow.mapper;

import com.custody.core.utils.JsonValues;
import com.custody.workflow.dto.response.ChangeRequestResponse;
import com.custody.workflow.dto.response.SetupRecordResponse;
import com.custody.workflow.model.ChangeRequest;
import com.custody.workflow.model.SetupRecord;
import org.springframework.stereotype.Component;

@Component
public class WorkflowResponseMapper {
  private final JsonValues json;

  public WorkflowResponseMapper(JsonValues json) {
    this.json = json;
  }

  public SetupRecordResponse toRecordResponse(SetupRecord setupRecord) {
    return new SetupRecordResponse(
        setupRecord.getId(),
        setupRecord.getFeature(),
        setupRecord.getScope(),
        setupRecord.getBusinessKey(),
        json.read(setupRecord.getData()),
        setupRecord.getStatus(),
        setupRecord.getVersion(),
        setupRecord.getUpdatedAt());
  }

  public ChangeRequestResponse toChangeResponse(ChangeRequest changeRequest) {
    return new ChangeRequestResponse(
        changeRequest.getId(),
        changeRequest.getFeature(),
        changeRequest.getScope(),
        changeRequest.getBusinessKey(),
        changeRequest.getRecordId(),
        changeRequest.getAction(),
        changeRequest.getStatus(),
        json.read(changeRequest.getBeforeData()),
        json.read(changeRequest.getProposedData()),
        changeRequest.getMaker(),
        changeRequest.getChecker(),
        changeRequest.getReason(),
        changeRequest.getDecisionReason(),
        changeRequest.getSubmittedAt(),
        changeRequest.getDecidedAt());
  }
}
