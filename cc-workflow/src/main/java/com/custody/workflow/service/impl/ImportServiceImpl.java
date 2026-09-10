package com.custody.workflow.service.impl;

import com.custody.core.dto.AuditDraft;
import com.custody.core.enums.Action;
import com.custody.core.enums.AuditCategory;
import com.custody.core.exception.AppException;
import com.custody.core.service.AuditWriter;
import com.custody.core.utils.Ids;
import com.custody.identity.service.AccessPolicy;
import com.custody.workflow.dto.ImportResult;
import com.custody.workflow.dto.request.CreateSetupRequest;
import com.custody.workflow.error.WorkflowErrorCatalog;
import com.custody.workflow.service.ImportService;
import com.custody.workflow.service.WorkflowService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportServiceImpl implements ImportService {

  private final WorkflowService workflow;
  private final AccessPolicy access;
  private final AuditWriter audit;

  public ImportServiceImpl(
      WorkflowService workflowService, AccessPolicy accessPolicy, AuditWriter audit) {
    this.workflow = workflowService;
    this.access = accessPolicy;
    this.audit = audit;
  }

  @Transactional
  @Override
  public ImportResult submit(
      String actor, String feature, String scope, List<CreateSetupRequest> rows) {
    access.require(actor, feature, Action.ADD, scope);
    AppException.require(!rows.isEmpty() && rows.size() <= 100, WorkflowErrorCatalog.INPUT_INVALID);
    String batch = Ids.next();
    List<String> changes = new ArrayList<>();
    for (var row : rows) {
      AppException.require(scope.equals(row.scope()), WorkflowErrorCatalog.INPUT_INVALID);
      changes.add(workflow.add(actor, feature, row, "Import " + batch).id());
    }
    audit.append(
        AuditDraft.builder(AuditCategory.BUSINESS)
            .feature(feature)
            .scope(scope)
            .recordId(batch)
            .reference(batch)
            .action("IMPORT")
            .status("SUBMITTED")
            .actor(actor)
            .maker(actor)
            .checker("")
            .reason("Imported additions awaiting individual approval")
            .source("CSV import")
            .before(Map.of())
            .after(Map.of("records", Integer.toString(rows.size())))
            .build());
    return new ImportResult(batch, rows.size(), List.copyOf(changes));
  }
}
