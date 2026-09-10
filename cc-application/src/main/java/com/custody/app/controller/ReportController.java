package com.custody.app.controller;

import com.custody.app.constants.ApiRoutes;
import com.custody.reporting.dto.request.CreateScheduleRequest;
import com.custody.reporting.dto.request.GenerateReportRequest;
import com.custody.reporting.dto.response.ReportResponse;
import com.custody.reporting.dto.response.ScheduleResponse;
import com.custody.reporting.service.ReportScheduleService;
import com.custody.reporting.service.ReportService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ReportController {
  private final ReportService reports;
  private final ReportScheduleService reportSchedules;

  ReportController(ReportService reports, ReportScheduleService reportSchedules) {
    this.reports = reports;
    this.reportSchedules = reportSchedules;
  }

  @GetMapping(ApiRoutes.REPORTS)
  List<ReportResponse> list(Principal principal) {
    return reports.list(principal.getName());
  }

  @PostMapping(ApiRoutes.REPORTS)
  @ResponseStatus(HttpStatus.ACCEPTED)
  ReportResponse create(Principal principal, @Valid @RequestBody GenerateReportRequest request) {
    return reports.request(principal.getName(), request);
  }

  @GetMapping(ApiRoutes.REPORT_DOWNLOAD)
  ResponseEntity<byte[]> download(Principal principal, @PathVariable String id) {
    var reportDownload = reports.download(principal.getName(), id);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(reportDownload.filename()).build().toString())
        .cacheControl(CacheControl.noStore())
        .contentType(MediaType.parseMediaType(reportDownload.contentType()))
        .body(reportDownload.bytes());
  }

  @GetMapping(ApiRoutes.SCHEDULES)
  List<ScheduleResponse> schedules(Principal principal) {
    return reportSchedules.schedules(principal.getName());
  }

  @PostMapping(ApiRoutes.SCHEDULES)
  ScheduleResponse schedule(Principal principal, @Valid @RequestBody CreateScheduleRequest input) {
    return reportSchedules.schedule(principal.getName(), input);
  }

  @PostMapping(ApiRoutes.SCHEDULE_TOGGLE)
  ScheduleResponse toggle(Principal principal, @PathVariable String id) {
    return reportSchedules.toggle(principal.getName(), id);
  }
}
