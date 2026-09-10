package com.custody.app.bootstrap;

import com.custody.identity.service.IdentityService;
import com.custody.reporting.dto.request.CreateScheduleRequest;
import com.custody.reporting.enums.ReportFormat;
import com.custody.reporting.service.ReportScheduleService;
import com.custody.workflow.dto.request.ApprovalDecisionRequest;
import com.custody.workflow.dto.request.CreateSetupRequest;
import com.custody.workflow.dto.request.UpdateSetupRequest;
import com.custody.workflow.service.WorkflowService;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("demo & dev & !sit & !uat & !prod")
class DemoBootstrap implements ApplicationRunner {
  private final IdentityService identity;
  private final PasswordEncoder encoder;
  private final String password;
  private final WorkflowService workflow;
  private final ReportScheduleService reports;

  DemoBootstrap(
      IdentityService identityService,
      PasswordEncoder passwordEncoder,
      @Value("${cc.demo.password}") String demoPassword,
      WorkflowService workflowService,
      ReportScheduleService reportScheduleService) {
    this.identity = identityService;
    this.encoder = passwordEncoder;
    this.password = demoPassword;
    this.workflow = workflowService;
    this.reports = reportScheduleService;
  }

  public void run(ApplicationArguments args) {
    identity.seed(password, encoder);
    if (!workflow.records("maker", "vault").isEmpty() || !workflow.pending("maker").isEmpty())
      return;
    Map<String, Map<String, String>> samples = new LinkedHashMap<>();
    samples.put(
        "vault",
        Map.of(
            "name",
            "Singapore Central Vault",
            "location",
            "Singapore Freeport",
            "operator",
            "Custody Operations"));
    samples.put(
        "holiday-calendar",
        Map.of("name", "National Day", "date", "2026-08-09", "market", "Singapore"));
    samples.put(
        "physical-gold",
        Map.of("name", "Good Delivery Gold Bar", "purity", "99.99", "unit", "Troy ounce"));
    samples.put(
        "loco-singapore",
        Map.of("name", "Singapore Settlement", "settlementCode", "SG-LOC", "currency", "USD"));
    samples.put(
        "fee-schedule",
        Map.of(
            "name",
            "Standard Storage",
            "rate",
            "0.15",
            "currency",
            "USD",
            "effectiveDate",
            "2026-09-01"));
    samples.put("reference-data", Map.of("name", "Gold", "category", "Metal", "value", "XAU"));
    int featureIndex = 1;
    for (var entry : samples.entrySet()) {
      var changeRequestResponse =
          workflow.add(
              "maker",
              entry.getKey(),
              new CreateSetupRequest(
                  "DEMO-00" + featureIndex++,
                  "BULLION",
                  entry.getValue(),
                  "Initial demonstration setup"),
              "Demo bootstrap");
      workflow.decide(
          "checker",
          changeRequestResponse.id(),
          new ApprovalDecisionRequest(true, "Reviewed demonstration data"));
    }
    var setupRecordResponse = workflow.records("maker", "vault").getFirst();
    var values = new HashMap<>(setupRecordResponse.values());
    values.put("operator", "Bullion Clearing Operations");
    workflow.edit(
        "maker",
        setupRecordResponse.id(),
        new UpdateSetupRequest(
            setupRecordResponse.version(),
            values,
            "Update vault operator following operating review"));
    reports.schedule(
        "maker",
        new CreateScheduleRequest(
            "Daily CSA administrator report",
            "ADMIN",
            "administration",
            "SYSTEM",
            ReportFormat.CSV,
            1,
            "csa"));
  }
}
