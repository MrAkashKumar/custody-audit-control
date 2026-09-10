package com.custody.app.controller;

import com.custody.app.constants.ApiRoutes;
import com.custody.core.enums.Action;
import com.custody.core.exception.AppException;
import com.custody.identity.service.AccessPolicy;
import com.custody.workflow.dto.ImportResult;
import com.custody.workflow.dto.request.CreateSetupRequest;
import com.custody.workflow.error.WorkflowErrorCatalog;
import com.custody.workflow.service.FeatureRegistry;
import com.custody.workflow.service.ImportService;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
class ImportController {
  private final ImportService imports;
  private final FeatureRegistry features;
  private final AccessPolicy access;

  ImportController(
      ImportService importService, FeatureRegistry featureRegistry, AccessPolicy accessPolicy) {
    this.imports = importService;
    this.features = featureRegistry;
    this.access = accessPolicy;
  }

  @PostMapping(ApiRoutes.FEATURE_IMPORT)
  ImportResult upload(
      Principal principal,
      @PathVariable String feature,
      @RequestParam String scope,
      @RequestPart MultipartFile file)
      throws IOException {
    access.require(principal.getName(), feature, Action.ADD, scope);
    AppException.require(file.getSize() <= 2_000_000, WorkflowErrorCatalog.INPUT_INVALID);
    var definition = features.get(feature);
    Set<String> expected = new HashSet<>(List.of("businessKey", "reason"));
    definition.fields().forEach(fieldDefinition -> expected.add(fieldDefinition.key()));
    List<CreateSetupRequest> rows = new ArrayList<>();
    try (var reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
        var csv =
            CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW)
                .get()
                .parse(reader)) {
      AppException.require(
          csv.getHeaderMap().keySet().equals(expected), WorkflowErrorCatalog.INPUT_INVALID);
      for (var row : csv) {
        AppException.require(
            rows.size() < 100 && row.isConsistent(), WorkflowErrorCatalog.INPUT_INVALID);
        Map<String, String> values = new TreeMap<>();
        definition
            .fields()
            .forEach(
                fieldDefinition ->
                    values.put(fieldDefinition.key(), row.get(fieldDefinition.key())));
        String key = row.get("businessKey"), reason = row.get("reason");
        AppException.require(
            key.matches("[A-Za-z0-9_-]{2,60}") && !reason.isBlank() && reason.length() <= 1000,
            WorkflowErrorCatalog.INPUT_INVALID);
        rows.add(new CreateSetupRequest(key, scope, values, reason));
      }
    } catch (IllegalArgumentException illegalArgumentException) {
      throw AppException.of(WorkflowErrorCatalog.INPUT_INVALID);
    }
    return imports.submit(principal.getName(), feature, scope, rows);
  }
}
