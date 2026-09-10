package com.custody.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.custody.app.config.properties.RetentionProperties;
import com.custody.app.constants.ApiRoutes;
import com.custody.app.error.SystemErrorCatalog;
import com.custody.app.scheduler.RetentionJob;
import com.custody.audit.dto.request.AuditSearchRequest;
import com.custody.audit.service.AuditService;
import com.custody.core.enums.Action;
import com.custody.core.enums.OperationalStatus;
import com.custody.core.exception.AppException;
import com.custody.core.service.AuditWriter;
import com.custody.core.utils.Ids;
import com.custody.identity.dto.request.SecurityChangeRequest;
import com.custody.identity.error.IdentityErrorCatalog;
import com.custody.identity.service.AccessPolicy;
import com.custody.identity.service.IdentityService;
import com.custody.reporting.dto.request.CreateScheduleRequest;
import com.custody.reporting.dto.request.GenerateReportRequest;
import com.custody.reporting.enums.ReportFormat;
import com.custody.reporting.service.ReportScheduleService;
import com.custody.reporting.service.ReportService;
import com.custody.reporting.service.ReportWorkerService;
import com.custody.workflow.dto.request.ApprovalDecisionRequest;
import com.custody.workflow.dto.request.ChangeSetupStatusRequest;
import com.custody.workflow.dto.request.CreateSetupRequest;
import com.custody.workflow.dto.request.UpdateSetupRequest;
import com.custody.workflow.dto.response.ChangeRequestResponse;
import com.custody.workflow.dto.response.SetupRecordResponse;
import com.custody.workflow.error.WorkflowErrorCatalog;
import com.custody.workflow.service.FeatureRegistry;
import com.custody.workflow.service.ImportService;
import com.custody.workflow.service.WorkflowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
    properties = {"spring.datasource.url=jdbc:h2:mem:cc_tests", "cc.jobs.poll-ms=3600000"})
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class ApplicationIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired WorkflowService workflow;
  @Autowired ReportService reports;
  @Autowired ReportWorkerService reportWorker;
  @Autowired ReportScheduleService reportSchedules;
  @Autowired IdentityService identities;
  @Autowired AccessPolicy access;
  @Autowired FeatureRegistry features;
  @Autowired RetentionJob retention;
  @Autowired AuditService audit;
  @MockitoSpyBean AuditWriter auditWriter;

  private CreateSetupRequest input(String key) {
    return new CreateSetupRequest(
        key,
        "BULLION",
        Map.of("name", "Test vault", "location", "Singapore", "operator", "Custody"),
        "Acceptance test");
  }

  private ChangeRequestResponse add() {
    return workflow.add("maker", "vault", input("T-" + Ids.next()), "Test");
  }

  private SetupRecordResponse approved() {
    var changeRequestResponse = add();
    var applied =
        workflow.decide(
            "checker", changeRequestResponse.id(), new ApprovalDecisionRequest(true, "Reviewed"));
    return workflow.records("maker", "vault").stream()
        .filter(setupRecordResponse -> setupRecordResponse.id().equals(applied.recordId()))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void additionDoesNotExistUntilIndependentApproval() {
    var changeRequestResponse = add();
    assertThat(workflow.records("maker", "vault"))
        .noneMatch(
            setupRecordResponse ->
                setupRecordResponse.businessKey().equals(changeRequestResponse.businessKey()));
    assertThatThrownBy(
            () ->
                workflow.decide(
                    "maker", changeRequestResponse.id(), new ApprovalDecisionRequest(true, "Self")))
        .isInstanceOfSatisfying(
            AppException.class,
            appException ->
                assertThat(appException.code())
                    .isEqualTo(WorkflowErrorCatalog.SELF_APPROVAL_FORBIDDEN.code()));
    workflow.decide(
        "checker", changeRequestResponse.id(), new ApprovalDecisionRequest(true, "Reviewed"));
    assertThat(workflow.records("maker", "vault"))
        .anyMatch(
            setupRecordResponse ->
                setupRecordResponse.businessKey().equals(changeRequestResponse.businessKey()));
  }

  @Test
  void editAndStatusPreserveApprovedValuesUntilDecision() {
    var setupRecordResponse = approved();
    var values = new HashMap<>(setupRecordResponse.values());
    values.put("location", "New location");
    var changeRequestResponse =
        workflow.edit(
            "maker",
            setupRecordResponse.id(),
            new UpdateSetupRequest(setupRecordResponse.version(), values, "Relocation"));
    assertThat(
            workflow.records("maker", "vault").stream()
                .filter(
                    candidateSetupRecordResponse ->
                        candidateSetupRecordResponse.id().equals(setupRecordResponse.id()))
                .findFirst()
                .orElseThrow()
                .values()
                .get("location"))
        .isEqualTo("Singapore");
    workflow.decide(
        "checker", changeRequestResponse.id(), new ApprovalDecisionRequest(false, "Rejected"));
    var status =
        workflow.status(
            "maker",
            setupRecordResponse.id(),
            new ChangeSetupStatusRequest(
                setupRecordResponse.version(), OperationalStatus.INACTIVE, "Closing"));
    workflow.decide("checker", status.id(), new ApprovalDecisionRequest(true, "Reviewed"));
    assertThat(
            workflow.records("maker", "vault").stream()
                .filter(
                    candidateSetupRecordResponse ->
                        candidateSetupRecordResponse.id().equals(setupRecordResponse.id()))
                .findFirst()
                .orElseThrow()
                .status())
        .isEqualTo(OperationalStatus.INACTIVE);
  }

  @Test
  void duplicateAndStaleRequestsAreRejected() {
    var setupRecordResponse = approved();
    var changeRequestResponse =
        workflow.status(
            "maker",
            setupRecordResponse.id(),
            new ChangeSetupStatusRequest(
                setupRecordResponse.version(), OperationalStatus.INACTIVE, "Closing"));
    assertThatThrownBy(
            () ->
                workflow.status(
                    "maker",
                    setupRecordResponse.id(),
                    new ChangeSetupStatusRequest(
                        setupRecordResponse.version(), OperationalStatus.INACTIVE, "Closing")))
        .isInstanceOf(AppException.class);
    workflow.decide(
        "checker", changeRequestResponse.id(), new ApprovalDecisionRequest(true, "Reviewed"));
    assertThatThrownBy(
            () ->
                workflow.edit(
                    "maker",
                    setupRecordResponse.id(),
                    new UpdateSetupRequest(
                        setupRecordResponse.version(),
                        Map.of("name", "Changed", "location", "Singapore", "operator", "Custody"),
                        "Edit")))
        .isInstanceOfSatisfying(
            AppException.class,
            appException ->
                assertThat(appException.code())
                    .isEqualTo(WorkflowErrorCatalog.STALE_VERSION.code()));
    assertThatThrownBy(
            () ->
                workflow.decide(
                    "checker",
                    changeRequestResponse.id(),
                    new ApprovalDecisionRequest(true, "Again")))
        .isInstanceOf(AppException.class);
  }

  @Test
  void repeatedMakerCheckerCyclesKeepCompleteAndTruthfulEvidence() {
    String businessKey = "HISTORY-" + Ids.next();
    var addition = workflow.add("maker", "vault", input(businessKey), "Test");
    workflow.decide("checker", addition.id(), new ApprovalDecisionRequest(true, "Create approved"));

    SetupRecordResponse record =
        workflow.records("maker", "vault").stream()
            .filter(candidate -> candidate.businessKey().equals(businessKey))
            .findFirst()
            .orElseThrow();
    Map<String, String> rejectedValues = new HashMap<>(record.values());
    rejectedValues.put("location", "Rejected location");
    var rejectedEdit =
        workflow.edit(
            "maker",
            record.id(),
            new UpdateSetupRequest(record.version(), rejectedValues, "First correction"));
    workflow.decide(
        "checker", rejectedEdit.id(), new ApprovalDecisionRequest(false, "Not acceptable"));

    Map<String, String> approvedValues = new HashMap<>(record.values());
    approvedValues.put("location", "Approved location");
    var approvedEdit =
        workflow.edit(
            "maker",
            record.id(),
            new UpdateSetupRequest(record.version(), approvedValues, "Second correction"));
    workflow.decide(
        "checker", approvedEdit.id(), new ApprovalDecisionRequest(true, "Correction approved"));

    var evidence =
        audit
            .search(
                "maker",
                new AuditSearchRequest(
                    "STATIC",
                    "vault",
                    businessKey,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "BULLION",
                    null,
                    null),
                0,
                100)
            .content();
    assertThat(evidence).hasSize(6);
    assertThat(evidence.stream().map(event -> event.reference()).distinct()).hasSize(3);

    var rejection =
        evidence.stream()
            .filter(event -> event.reference().equals(rejectedEdit.id()))
            .filter(event -> event.status().equals("REJECTED"))
            .findFirst()
            .orElseThrow();
    assertThat(rejection.proposed()).containsEntry("location", "Rejected location");
    assertThat(rejection.after()).containsEntry("location", "Singapore");

    var approval =
        evidence.stream()
            .filter(event -> event.reference().equals(approvedEdit.id()))
            .filter(event -> event.status().equals("APPROVED"))
            .findFirst()
            .orElseThrow();
    assertThat(approval.proposed()).containsEntry("location", "Approved location");
    assertThat(approval.after()).containsEntry("location", "Approved location");
  }

  @Test
  void crossGroupDataAndReportsAreHidden() {
    var setupRecordResponse = approved();
    assertThat(workflow.records("other", "vault"))
        .noneMatch(
            candidateSetupRecordResponse ->
                candidateSetupRecordResponse.id().equals(setupRecordResponse.id()));
    assertThatThrownBy(
            () ->
                workflow.edit(
                    "other",
                    setupRecordResponse.id(),
                    new UpdateSetupRequest(
                        setupRecordResponse.version(), setupRecordResponse.values(), "Attempt")))
        .isInstanceOfSatisfying(
            AppException.class,
            appException ->
                assertThat(appException.code())
                    .isEqualTo(WorkflowErrorCatalog.RESOURCE_NOT_AVAILABLE.code()));
  }

  @Test
  void auditFailureRollsBackProposalAndApplication() {
    var changeRequestResponse = add();
    AuditWriter target =
        org.springframework.test.util.AopTestUtils.getUltimateTargetObject(auditWriter);
    doThrow(new IllegalStateException("simulated sink failure")).when(target).append(any());
    try {
      assertThatThrownBy(
              () ->
                  workflow.decide(
                      "checker",
                      changeRequestResponse.id(),
                      new ApprovalDecisionRequest(true, "Reviewed")))
          .isInstanceOf(IllegalStateException.class);
      assertThat(workflow.records("maker", "vault"))
          .noneMatch(
              setupRecordResponse ->
                  setupRecordResponse.businessKey().equals(changeRequestResponse.businessKey()));
      assertThat(workflow.pending("maker"))
          .anyMatch(
              pendingChangeRequest -> pendingChangeRequest.id().equals(changeRequestResponse.id()));
    } finally {
      reset(target);
    }
  }

  @Test
  void applicationDatabaseRoleCannotAlterEvidence() throws Exception {
    add();
    try (Connection connection =
            DriverManager.getConnection("jdbc:h2:mem:cc_tests", "cc_app", "local-app-change-me");
        Statement statement = connection.createStatement()) {
      assertThatThrownBy(() -> statement.executeUpdate("delete from cc_audit_event"))
          .isInstanceOf(SQLException.class);
      assertThatThrownBy(
              () -> statement.executeUpdate("update cc_audit_event set reason='tampered' "))
          .isInstanceOf(SQLException.class);
    }
  }

  @Test
  void csvAndExcelAreRealSecureArtifacts() throws Exception {
    approved();
    var auditSearchRequest =
        new AuditSearchRequest(
            "STATIC", "vault", null, null, null, null, null, null, null, "BULLION", null, null);
    for (var format : ReportFormat.values()) {
      var job = reports.request("maker", new GenerateReportRequest(format, auditSearchRequest));
      reportWorker.process(job.id());
      var reportDownload = reports.download("maker", job.id());
      assertThat(reportDownload.bytes()).isNotEmpty();
      if (format == ReportFormat.CSV)
        assertThat(new String(reportDownload.bytes(), java.nio.charset.StandardCharsets.UTF_8))
            .contains("Selection criteria", "Before", "After");
      else
        try (var workbook =
            new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                new ByteArrayInputStream(reportDownload.bytes()))) {
          assertThat(workbook.getSheet("Audit evidence").getLastRowNum()).isPositive();
        }
      assertThatThrownBy(() -> reports.download("other", job.id()))
          .isInstanceOf(AppException.class);
    }
  }

  @Test
  void revokedMembershipImmediatelyRemovesAccess() {
    var group = "test-" + Ids.next();
    var request =
        identities.propose(
            "maker",
            new SecurityChangeRequest(
                "GROUP",
                Map.of("groupId", group, "name", "Temporary", "active", "true"),
                "Create"));
    identities.decide("checker", request.id(), true, "Reviewed");
    var grant =
        identities.propose(
            "maker",
            new SecurityChangeRequest(
                "GRANT",
                Map.of(
                    "groupId",
                    group,
                    "feature",
                    "vault",
                    "action",
                    "VIEW",
                    "scope",
                    "ISOLATED",
                    "operation",
                    "ADD"),
                "Grant"));
    identities.decide("checker", grant.id(), true, "Reviewed");
    var join =
        identities.propose(
            "maker",
            new SecurityChangeRequest(
                "MEMBERSHIP",
                Map.of("userId", "other", "groupId", group, "operation", "ADD"),
                "Join"));
    assertThat(access.allows("other", "vault", Action.VIEW, "ISOLATED")).isFalse();
    identities.decide("checker", join.id(), true, "Reviewed");
    assertThat(access.allows("other", "vault", Action.VIEW, "ISOLATED")).isTrue();
    var leave =
        identities.propose(
            "maker",
            new SecurityChangeRequest(
                "MEMBERSHIP",
                Map.of("userId", "other", "groupId", group, "operation", "REMOVE"),
                "Leave"));
    identities.decide("checker", leave.id(), true, "Reviewed");
    assertThat(access.allows("other", "vault", Action.VIEW, "ISOLATED")).isFalse();
  }

  @Test
  void allSixFeatureDefinitionsValidateAndRejectUnknownFields() {
    assertThat(features.all()).hasSize(6);
    for (var featureDefinition : features.all()) {
      Map<String, String> input = new HashMap<>();
      featureDefinition
          .fields()
          .forEach(
              field ->
                  input.put(
                      field.key(),
                      switch (field.type()) {
                        case "date" -> "2026-09-09";
                        case "decimal" -> "1.2500";
                        default -> "Example";
                      }));
      assertThat(featureDefinition.validate(input)).hasSize(featureDefinition.fields().size());
      input.put("checker", "spoofed");
      assertThatThrownBy(() -> featureDefinition.validate(input)).isInstanceOf(AppException.class);
    }
  }

  @Test
  void emptyReportIsExplicitNoActivity() {
    var auditSearchRequest =
        new AuditSearchRequest(
            "STATIC",
            "vault",
            "NONEXISTENT",
            null,
            null,
            null,
            null,
            null,
            null,
            "BULLION",
            null,
            null);
    var reportResponse =
        reports.request("maker", new GenerateReportRequest(ReportFormat.CSV, auditSearchRequest));
    reportWorker.process(reportResponse.id());
    assertThat(
            new String(
                reports.download("maker", reportResponse.id()).bytes(),
                java.nio.charset.StandardCharsets.UTF_8))
        .contains("No activity");
  }

  @Test
  void unauthenticatedErrorsHaveMatchingTraceIds() throws Exception {
    MvcResult mvcResult =
        mvc.perform(get(ApiRoutes.CHANGES).header(ApiRoutes.TRACE_HEADER, "untrusted"))
            .andExpect(status().isUnauthorized())
            .andExpect(
                jsonPath("$.code").value(IdentityErrorCatalog.AUTHENTICATION_REQUIRED.code()))
            .andReturn();
    String trace = mvcResult.getResponse().getHeader(ApiRoutes.TRACE_HEADER);
    assertThat(trace).matches("[0-9a-f]{32}");
    assertThat(json.readTree(mvcResult.getResponse().getContentAsString()).get("traceId").asText())
        .isEqualTo(trace);
  }

  @Test
  void loginSuccessFailureAndRepeatedSuspiciousAttemptsAreAudited() throws Exception {
    mvc.perform(
            post(ApiRoutes.LOGIN)
                .with(csrf())
                .param("username", "maker")
                .param("password", "CustodyDemo!2026"))
        .andExpect(status().isOk());
    for (int attempt = 0; attempt < 5; attempt++)
      mvc.perform(
              post(ApiRoutes.LOGIN)
                  .with(csrf())
                  .param("username", "maker")
                  .param("password", "incorrect-password"))
          .andExpect(status().isUnauthorized());

    var loginEvidence =
        audit
            .search(
                "maker",
                new AuditSearchRequest(
                    "SECURITY",
                    "administration",
                    "maker",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "SYSTEM",
                    null,
                    null),
                0,
                100)
            .content();
    assertThat(loginEvidence)
        .anyMatch(
            event ->
                event.eventType().equals("AUTHENTICATION_SUCCESS")
                    && event.endpoint().equals(ApiRoutes.LOGIN)
                    && event.httpStatus() == 200)
        .anyMatch(
            event ->
                event.eventType().equals("AUTHENTICATION_FAILURE")
                    && event.outcome().equals("FAILURE"))
        .anyMatch(
            event ->
                event.eventType().equals("SUSPICIOUS_ACTIVITY")
                    && event.riskLevel().equals("HIGH"));
  }

  @Test
  void csrfAndDtoValidationAreEnforced() throws Exception {
    mvc.perform(
            post(ApiRoutes.FEATURES + "/vault/records")
                .with(user("maker"))
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post(ApiRoutes.FEATURES + "/vault/records")
                .with(user("maker"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"maker\":\"spoofed\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(SystemErrorCatalog.INPUT_INVALID.code()));

    assertThat(
            audit
                .search(
                    "maker",
                    new AuditSearchRequest(
                        "SECURITY",
                        "administration",
                        ApiRoutes.FEATURE_RECORDS,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "SYSTEM",
                        null,
                        null),
                    0,
                    100)
                .content())
        .anyMatch(
            event ->
                event.eventType().equals("VALIDATION_FAILURE")
                    && event.errorCode().equals(SystemErrorCatalog.INPUT_INVALID.code())
                    && event.httpStatus() == 400
                    && event.actor().equals("maker"));
  }

  @Test
  void reversedDatesAreValidationErrors() throws Exception {
    mvc.perform(
            get(ApiRoutes.AUDIT)
                .with(user("maker"))
                .param("from", "2026-09-10")
                .param("to", "2026-09-01"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void frameworkFailuresUseTheSameSafeProblemContract() throws Exception {
    mvc.perform(get(ApiRoutes.BASE + "/does-not-exist").with(user("maker")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(SystemErrorCatalog.ENDPOINT_NOT_FOUND.code()))
        .andExpect(jsonPath("$.traceId").isNotEmpty());

    mvc.perform(post(ApiRoutes.FEATURES).with(user("maker")).with(csrf()))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value(SystemErrorCatalog.REQUEST_METHOD_NOT_ALLOWED.code()));

    mvc.perform(
            post(ApiRoutes.FEATURES + "/vault/records")
                .with(user("maker"))
                .with(csrf())
                .contentType("application/xml")
                .content("<record/>"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value(SystemErrorCatalog.MEDIA_TYPE_NOT_SUPPORTED.code()));
  }

  @Test
  void retentionDryRunDoesNotDeleteEvidence() {
    long before = audit.search("maker", AuditSearchRequest.empty(), 0, 1).totalElements();
    assertThat(retention.runOnce()).isZero();
    assertThat(audit.search("maker", AuditSearchRequest.empty(), 0, 1).totalElements())
        .isEqualTo(before);
  }

  @Test
  void scheduledDefinitionIsAuditedAndPermissionChecked() {
    var scheduleResponse =
        reportSchedules.schedule(
            "maker",
            new CreateScheduleRequest(
                "Daily CSA", "ADMIN", "administration", "SYSTEM", ReportFormat.CSV, 1, "csa"));
    assertThat(scheduleResponse.nextRunAt()).isAfter(Instant.now());
    assertThatThrownBy(
            () ->
                reportSchedules.schedule(
                    "other",
                    new CreateScheduleRequest(
                        "Not permitted",
                        "ADMIN",
                        "administration",
                        "SYSTEM",
                        ReportFormat.CSV,
                        1,
                        null)))
        .isInstanceOf(AppException.class);
  }

  @Test
  void allSixFeaturesUseTheSameApprovalContract() {
    for (var featureDefinition : features.all()) {
      Map<String, String> values = new HashMap<>();
      featureDefinition
          .fields()
          .forEach(
              field ->
                  values.put(
                      field.key(),
                      switch (field.type()) {
                        case "date" -> "2026-09-09";
                        case "decimal" -> "1.25";
                        default -> "Example";
                      }));
      var request =
          workflow.add(
              "maker",
              featureDefinition.key(),
              new CreateSetupRequest("ALL-" + Ids.next(), "BULLION", values, "Feature coverage"),
              "Test");
      var applied =
          workflow.decide("checker", request.id(), new ApprovalDecisionRequest(true, "Reviewed"));
      assertThat(workflow.records("maker", featureDefinition.key()))
          .anyMatch(setupRecordResponse -> setupRecordResponse.id().equals(applied.recordId()));
    }
  }

  @Test
  void actualRetentionDeletesOnlyOlderThanCutoff() throws Exception {
    Instant now = Instant.parse("2026-09-09T00:00:00Z");
    String expired = Ids.next(), boundary = Ids.next();
    try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:cc_tests", "sa", "");
        var statement =
            connection.prepareStatement(
                "insert into cc_audit_event(id,occurred_at,before_values,after_values) values(?,?,?,?)")) {
      for (String id : List.of(expired, boundary)) {
        statement.setString(1, id);
        statement.setTimestamp(
            2,
            Timestamp.from(
                Instant.parse("2019-09-09T00:00:00Z").minusSeconds(id.equals(expired) ? 1 : 0)));
        statement.setString(3, "{}");
        statement.setString(4, "{}");
        statement.executeUpdate();
      }
      var job =
          new RetentionJob(
              new RetentionProperties(
                  7,
                  7,
                  true,
                  false,
                  1000,
                  "0 0 3 * * *",
                  "cc_retention",
                  "local-retention-change-me"),
              Clock.fixed(now, ZoneOffset.UTC),
              "jdbc:h2:mem:cc_tests");
      assertThat(job.runOnce()).isEqualTo(1);
      try (var preparedStatement =
          connection.prepareStatement("select count(*) from cc_audit_event where id=?")) {
        preparedStatement.setString(1, boundary);
        try (var result = preparedStatement.executeQuery()) {
          result.next();
          assertThat(result.getInt(1)).isEqualTo(1);
        }
      }
      try (var remove = connection.prepareStatement("delete from cc_audit_event where id=?")) {
        remove.setString(1, boundary);
        remove.executeUpdate();
      }
    }
  }

  @Test
  void newFieldRestrictionHidesEvidenceAndDeniesOldArtifact() {
    approved();
    var report =
        reports.request(
            "maker",
            new GenerateReportRequest(
                ReportFormat.CSV,
                new AuditSearchRequest(
                    "STATIC", "vault", null, null, null, null, null, null, null, "BULLION", null,
                    null)));
    reportWorker.process(report.id());
    String group = "restricted-" + Ids.next();
    approveSecurity(
        "GROUP", Map.of("groupId", group, "name", "Restricted fields", "active", "true"));
    for (String action : List.of("AUDIT", "DOWNLOAD"))
      approveSecurity(
          "GRANT",
          Map.of(
              "groupId",
              group,
              "feature",
              "vault",
              "action",
              action,
              "scope",
              "BULLION",
              "operation",
              "ADD",
              "hiddenFields",
              "location"));
    approveSecurity("MEMBERSHIP", Map.of("groupId", group, "userId", "maker", "operation", "ADD"));
    try {
      assertThat(
              audit
                  .search(
                      "maker",
                      new AuditSearchRequest(
                          "STATIC", "vault", null, null, null, null, null, null, null, null, null,
                          null),
                      0,
                      500)
                  .content())
          .allSatisfy(
              auditEventResponse -> {
                assertThat(auditEventResponse.before()).doesNotContainKey("location");
                assertThat(auditEventResponse.proposed()).doesNotContainKey("location");
                assertThat(auditEventResponse.after()).doesNotContainKey("location");
              });
      assertThatThrownBy(() -> reports.download("maker", report.id()))
          .isInstanceOf(AppException.class);
    } finally {
      approveSecurity(
          "MEMBERSHIP", Map.of("groupId", group, "userId", "maker", "operation", "REMOVE"));
    }
  }

  private void approveSecurity(String kind, Map<String, String> values) {
    var securityChangeResponse =
        identities.propose(
            "maker", new SecurityChangeRequest(kind, values, "Test security policy"));
    identities.decide("checker", securityChangeResponse.id(), true, "Reviewed");
  }

  @Test
  void concurrentApprovalAppliesExactlyOnce() throws Exception {
    var change = add();
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var gate = new java.util.concurrent.CountDownLatch(1);
      java.util.concurrent.Callable<Boolean> task =
          () -> {
            gate.await();
            try {
              workflow.decide(
                  "checker", change.id(), new ApprovalDecisionRequest(true, "Concurrent review"));
              return true;
            } catch (RuntimeException runtimeException) {
              return false;
            }
          };
      var firstApprovalResult = executor.submit(task);
      var secondApprovalResult = executor.submit(task);
      gate.countDown();
      assertThat((firstApprovalResult.get() ? 1 : 0) + (secondApprovalResult.get() ? 1 : 0))
          .isEqualTo(1);
      assertThat(
              workflow.records("maker", "vault").stream()
                  .filter(
                      setupRecordResponse ->
                          setupRecordResponse.businessKey().equals(change.businessKey()))
                  .count())
          .isEqualTo(1);
    }
  }

  @Test
  void scheduleOccurrenceIsIdempotentAndBuildsNullReport() throws Exception {
    var schedule =
        reportSchedules.schedule(
            "maker",
            new CreateScheduleRequest(
                "Historical empty day",
                "ADMIN",
                "administration",
                "SYSTEM",
                ReportFormat.CSV,
                1,
                "csa"));
    try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:cc_tests", "sa", "");
        var sql =
            connection.prepareStatement("update cc_report_schedule set next_run_at=? where id=?")) {
      sql.setTimestamp(1, Timestamp.from(Instant.parse("2020-01-02T00:00:00Z")));
      sql.setString(2, schedule.id());
      sql.executeUpdate();
    }
    reportSchedules.dispatch(schedule.id());
    var job =
        reports.list("maker").stream()
            .filter(reportResponse -> reportResponse.trigger().equals("SCHEDULED"))
            .findFirst()
            .orElseThrow();
    reportWorker.process(job.id());
    assertThat(
            new String(
                reports.download("maker", job.id()).bytes(),
                java.nio.charset.StandardCharsets.UTF_8))
        .contains("No activity");
    reportSchedules.toggle("maker", schedule.id());
  }

  @Autowired ImportService imports;

  @Test
  void importsAreAtomicPendingProposals() {
    String key = "IMP-" + Ids.next();
    var invalid =
        new CreateSetupRequest(
            "BAD-" + Ids.next(),
            "BULLION",
            Map.of("name", "", "location", "SG", "operator", "Custody"),
            "Import test");
    assertThatThrownBy(
            () -> imports.submit("maker", "vault", "BULLION", List.of(input(key), invalid)))
        .isInstanceOf(AppException.class);
    assertThat(workflow.pending("maker"))
        .noneMatch(changeRequestResponse -> changeRequestResponse.businessKey().equals(key));
    var result =
        imports.submit(
            "maker", "vault", "BULLION", List.of(input(key), input("IMP-" + Ids.next())));
    assertThat(result.submitted()).isEqualTo(2);
    assertThat(workflow.records("maker", "vault"))
        .noneMatch(setupRecordResponse -> setupRecordResponse.businessKey().equals(key));
  }

  @Test
  void exportEscapesFormulaLookingValues() {
    String key = "CSV-" + Ids.next();
    var changeRequestResponse =
        workflow.add(
            "maker",
            "vault",
            new CreateSetupRequest(
                key,
                "BULLION",
                Map.of("name", "=SUM(1,2)", "location", "SG", "operator", "Custody"),
                "Safe export"),
            "Test");
    workflow.decide(
        "checker", changeRequestResponse.id(), new ApprovalDecisionRequest(true, "Reviewed"));
    var auditSearchRequest =
        new AuditSearchRequest(
            "STATIC", "vault", key, null, null, null, null, null, null, "BULLION", null, null);
    var job =
        reports.request("maker", new GenerateReportRequest(ReportFormat.CSV, auditSearchRequest));
    reportWorker.process(job.id());
    assertThat(
            new String(
                reports.download("maker", job.id()).bytes(),
                java.nio.charset.StandardCharsets.UTF_8))
        .contains("'=SUM(1,2)");
  }

  @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
  @Autowired org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

  private com.custody.core.dto.AuditDraft captureTestDraft(String reference) {
    return com.custody.core.dto.AuditDraft.builder(com.custody.core.enums.AuditCategory.BUSINESS)
        .feature("vault")
        .scope("BULLION")
        .recordId(reference)
        .reference(reference)
        .action("CAPTURE_TEST")
        .status("COMPLETED")
        .actor("maker")
        .maker("maker")
        .reason("Audit capture verification")
        .source("Test")
        .before(Map.of())
        .after(Map.of())
        .build();
  }

  @Test
  void auditWritingRequiresAnExistingTransaction() {
    assertThatThrownBy(() -> auditWriter.append(captureTestDraft(Ids.next())))
        .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
  }

  @Test
  void writerPreservesContextAndGeneratesDistinctFallbackTraceIds() {
    String contextualReference = Ids.next();
    String firstReference = Ids.next();
    String secondReference = Ids.next();
    String traceId = Ids.next();
    var transaction =
        new org.springframework.transaction.support.TransactionTemplate(transactionManager);
    try (org.slf4j.MDC.MDCCloseable ignored = org.slf4j.MDC.putCloseable("traceId", traceId)) {
      transaction.executeWithoutResult(
          status -> auditWriter.append(captureTestDraft(contextualReference)));
    }
    transaction.executeWithoutResult(
        status -> {
          auditWriter.append(captureTestDraft(firstReference));
          auditWriter.append(captureTestDraft(secondReference));
        });
    assertThat(auditTrace(contextualReference)).isEqualTo(traceId);
    assertThat(auditTrace(firstReference))
        .matches("[0-9a-f]{32}")
        .isNotEqualTo(auditTrace(secondReference));
    assertThat(org.slf4j.MDC.get("traceId")).isNull();
  }

  private String auditTrace(String reference) {
    return jdbcTemplate.queryForObject(
        "select trace_id from cc_audit_event where reference_id = ?", String.class, reference);
  }

  @Test
  void rollbackRemovesEvidenceAlongWithTheBusinessTransaction() {
    String reference = Ids.next();
    var transaction =
        new org.springframework.transaction.support.TransactionTemplate(transactionManager);
    transaction.executeWithoutResult(
        status -> {
          auditWriter.append(captureTestDraft(reference));
          status.setRollbackOnly();
        });
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from cc_audit_event where reference_id = ?",
                Integer.class,
                reference))
        .isZero();
  }

  @Autowired com.custody.core.service.AuditCapture auditCapture;

  @Test
  void pluggableCaptureRequiresTheBusinessTransaction() {
    assertThatThrownBy(() -> auditCapture.record("unregistered-event"))
        .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
  }
}
