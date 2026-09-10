package com.custody.workflow.service;

import com.custody.workflow.dto.ImportResult;
import com.custody.workflow.dto.request.CreateSetupRequest;
import java.util.List;

/** Public use-case boundary for the ImportService capability. */
public interface ImportService {
  ImportResult submit(String actor, String feature, String scope, List<CreateSetupRequest> rows);
}
